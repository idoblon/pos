package com.springboot.POS.service;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Store;
import com.springboot.POS.repository.BranchRepository;
import com.springboot.POS.repository.ProductRepository;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Central enforcement for subscription plan limits, so caps live next to the
 * catalog instead of scattered across services. All methods throw
 * {@link UserException} (unchecked) with an upgrade-oriented message.
 *
 * <p>Counting rules: closed branches ({@code deleted=true}) and deleted
 * accounts free their slot; storage counts live product media bytes.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionLimitService {

    private final StoreRepository storeRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /** Bytes in a gigabyte for quota math. */
    public static final long BYTES_PER_GB = 1024L * 1024L * 1024L;

    private static String planLabel(String plan) {
        return plan != null && !plan.isBlank() ? plan.toUpperCase() : "BASIC";
    }

    public void requireBranchCapacity(Store store) {
        String plan = store != null ? store.getSubscriptionPlan() : null;
        int max = SubscriptionPlanCatalog.maxBranchesOf(plan);
        int used = store != null
                ? branchRepository.findByStoreIdAndDeletedFalse(store.getId()).size()
                : 0;
        if (used >= max) {
            throw new UserException("Branch limit reached for the " + planLabel(plan)
                    + " plan (" + used + "/" + max + " branches). "
                    + "Upgrade your subscription to add more branches.");
        }
    }

    public void requireUserCapacity(Store store) {
        String plan = store != null ? store.getSubscriptionPlan() : null;
        int max = SubscriptionPlanCatalog.maxUsersOf(plan);
        int used = store != null
                ? userRepository.findByStore_IdAndDeletedFalse(store.getId()).size()
                : 0;
        if (used >= max) {
            throw new UserException("User limit reached for the " + planLabel(plan)
                    + " plan (" + used + "/" + max + " users). "
                    + "Upgrade your subscription to add more staff.");
        }
    }

    /** Live media bytes used by a store (product images; base64 ≈ 1 char = 1 byte). */
    public long storageUsageBytes(Long storeId) {
        if (storeId == null) return 0L;
        Long bytes = productRepository.sumImageBytesByStoreId(storeId);
        return bytes != null ? bytes : 0L;
    }

    /**
     * Rejects a product image that would push the store over its plan quota.
     * Text fields are negligible next to images, so only media is metered.
     */
    public void requireStorageFor(Store store, long newBytes) {
        String plan = store != null ? store.getSubscriptionPlan() : null;
        long quotaBytes = (long) SubscriptionPlanCatalog.storageGBOf(plan) * BYTES_PER_GB;
        long usedBytes = store != null ? storageUsageBytes(store.getId()) : 0L;
        if (usedBytes + Math.max(newBytes, 0) > quotaBytes) {
            throw new UserException("Storage quota exceeded for the " + planLabel(plan)
                    + " plan (" + SubscriptionPlanCatalog.storageGBOf(plan) + "GB). "
                    + "Remove unused product images or upgrade your subscription.");
        }
    }

    /**
     * Replacement variant: the old media is freed, so only the net growth
     * counts against the quota.
     */
    public void requireStorageForReplacement(Store store, long oldBytes, long newBytes) {
        String plan = store != null ? store.getSubscriptionPlan() : null;
        long quotaBytes = (long) SubscriptionPlanCatalog.storageGBOf(plan) * BYTES_PER_GB;
        long usedBytes = store != null ? storageUsageBytes(store.getId()) : 0L;
        long afterBytes = usedBytes - Math.max(oldBytes, 0) + Math.max(newBytes, 0);
        if (afterBytes > quotaBytes) {
            throw new UserException("Storage quota exceeded for the " + planLabel(plan)
                    + " plan (" + SubscriptionPlanCatalog.storageGBOf(plan) + "GB). "
                    + "Remove unused product images or upgrade your subscription.");
        }
    }

    /**
     * Caps stores per owner email: BASIC/PROFESSIONAL allow 1, ENTERPRISE is
     * unlimited. Counts live stores reached via the owner's contact email.
     */
    public void requireStoreCreationAllowed(String ownerEmail, String plan) {
        int max = SubscriptionPlanCatalog.maxStoresOf(plan);
        if (max == SubscriptionPlanCatalog.UNLIMITED_STORES) return;
        long owned = ownerEmail != null && !ownerEmail.isBlank()
                ? storeRepository.countByContactEmailIgnoreCase(ownerEmail.trim())
                : 0L;
        if (owned >= max) {
            throw new UserException("Store limit reached for the " + planLabel(plan)
                    + " plan (" + owned + "/" + max + " store). "
                    + "Upgrade to Enterprise for unlimited stores.");
        }
    }

    /** Premium feature gates: advancedAnalytics, warehouseTransfers, auditLog, apiAccess, whiteLabel. */
    public void requireFeature(Store store, String feature) {
        String plan = store != null ? store.getSubscriptionPlan() : null;
        if (!SubscriptionPlanCatalog.hasFeature(plan, feature)) {
            throw new UserException("This feature requires a higher subscription plan "
                    + "(current: " + planLabel(plan) + "). "
                    + "Upgrade your subscription to unlock it.");
        }
    }
}
