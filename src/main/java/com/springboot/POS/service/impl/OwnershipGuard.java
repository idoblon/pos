package com.springboot.POS.service.impl;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.ResourceAccessDeniedException;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.User;
import com.springboot.POS.repository.BranchRepository;
import com.springboot.POS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OwnershipGuard {

    private static final Logger log = LoggerFactory.getLogger(OwnershipGuard.class);

    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    // Throws if the user does not belong to the given store
    public void requireStoreAccess(User user, Long storeId) throws UserException {
        if (user.getRole() == UserRole.ROLE_ADMIN) return;
        if (storeId == null) {
            throw new ResourceAccessDeniedException("Access denied: store id is required");
        }
        Long userStoreId = resolveStoreId(user);
        if (userStoreId == null || !userStoreId.equals(storeId)) {
            log.debug("requireStoreAccess denied for role {} (userStoreId={}, requestedStoreId={})",
                    user.getRole(), userStoreId, storeId);
            throw new ResourceAccessDeniedException("Access denied: resource does not belong to your store");
        }
    }

    // Throws if the user does not belong to the given branch
    public void requireBranchAccess(User user, Long branchId) throws UserException {
        if (user.getRole() == UserRole.ROLE_ADMIN) return;
        if (branchId == null) {
            throw new ResourceAccessDeniedException("Access denied: branch id is required");
        }
        if (user.getRole() == UserRole.ROLE_STORE_ADMIN || user.getRole() == UserRole.ROLE_STORE_MANAGER) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new ResourceAccessDeniedException("Branch not found"));
            Long userStoreId = resolveStoreId(user);
            if (userStoreId == null || !branch.getStore().getId().equals(userStoreId)) {
                throw new ResourceAccessDeniedException("Access denied: branch does not belong to your store");
            }
            return;
        }
        if (user.getBranch() == null || !user.getBranch().getId().equals(branchId)) {
            throw new ResourceAccessDeniedException("Access denied: you are not assigned to this branch");
        }
    }

    /**
     * Throws unless the requester may act on the target user: self, ADMIN,
     * or a store superior (store admin/manager, branch manager) of a user in
     * the same store. Branch cashiers may only access their own record —
     * same-store lateral reads by cashiers are denied.
     */
    public void requireUserAccess(User requester, Long targetUserId) throws UserException {
        if (requester.getRole() == UserRole.ROLE_ADMIN) return;
        if (targetUserId == null) {
            throw new ResourceAccessDeniedException("Access denied: user id is required");
        }
        if (Objects.equals(requester.getId(), targetUserId)) return;

        Optional<User> target = userRepository.findById(targetUserId);
        if (target.isEmpty()) {
            throw new ResourceAccessDeniedException("User not found");
        }
        Long requesterStoreId = resolveStoreId(requester);
        Long targetStoreId = resolveStoreId(target.get());
        if (requesterStoreId == null || targetStoreId == null || !requesterStoreId.equals(targetStoreId)) {
            log.debug("requireUserAccess denied for role {}", requester.getRole());
            throw new ResourceAccessDeniedException("Access denied: user does not belong to your store");
        }
        if (requester.getRole() == UserRole.ROLE_STORE_ADMIN
                || requester.getRole() == UserRole.ROLE_STORE_MANAGER
                || requester.getRole() == UserRole.ROLE_BRANCH_MANAGER) {
            return;
        }
        log.debug("requireUserAccess denied lateral access for role {}", requester.getRole());
        throw new ResourceAccessDeniedException("Access denied: you may only access your own user record");
    }

    private Long resolveStoreId(User user) {
        if (user.getStore() != null) return user.getStore().getId();
        if (user.getBranch() != null && user.getBranch().getStore() != null)
            return user.getBranch().getStore().getId();
        return null;
    }

    /** Resolves the store a user belongs to (via store or branch), or null. */
    public Long resolveStoreIdOf(User user) {
        return resolveStoreId(user);
    }
}
