package com.springboot.POS.service.impl;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.ResourceAccessDeniedException;
import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.repository.BranchRepository;
import com.springboot.POS.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnershipGuardTest {

    private BranchRepository branchRepository;
    private UserRepository userRepository;
    private OwnershipGuard guard;

    @BeforeEach
    void setUp() {
        branchRepository = mock(BranchRepository.class);
        userRepository = mock(UserRepository.class);
        guard = new OwnershipGuard(branchRepository, userRepository);
    }

    private static Store store(long id) {
        Store store = new Store();
        store.setId(id);
        return store;
    }

    private static Branch branch(long id, Store store) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setStore(store);
        return branch;
    }

    private static User user(long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }

    // ---- requireStoreAccess ----

    @Test
    void adminBypassesStoreCheck() {
        User admin = user(1, UserRole.ROLE_ADMIN);
        assertDoesNotThrow(() -> guard.requireStoreAccess(admin, 42L));
        assertDoesNotThrow(() -> guard.requireStoreAccess(admin, null));
    }

    @Test
    void missingStoreIdDeniedForNonAdmin() {
        User storeAdmin = user(1, UserRole.ROLE_STORE_ADMIN);
        storeAdmin.setStore(store(5));
        assertThrows(ResourceAccessDeniedException.class,
                () -> guard.requireStoreAccess(storeAdmin, null));
    }

    @Test
    void ownStorePasses() {
        User storeAdmin = user(1, UserRole.ROLE_STORE_ADMIN);
        storeAdmin.setStore(store(5));
        assertDoesNotThrow(() -> guard.requireStoreAccess(storeAdmin, 5L));
    }

    @Test
    void foreignStoreDenied() {
        User storeAdmin = user(1, UserRole.ROLE_STORE_ADMIN);
        storeAdmin.setStore(store(5));
        assertThrows(ResourceAccessDeniedException.class,
                () -> guard.requireStoreAccess(storeAdmin, 6L));
    }

    @Test
    void storelessNonAdminDenied() {
        User cashier = user(1, UserRole.ROLE_BRANCH_CASHIER);
        assertThrows(ResourceAccessDeniedException.class,
                () -> guard.requireStoreAccess(cashier, 5L));
    }

    // ---- requireBranchAccess ----

    @Test
    void adminBypassesBranchCheck() {
        User admin = user(1, UserRole.ROLE_ADMIN);
        assertDoesNotThrow(() -> guard.requireBranchAccess(admin, 9L));
    }

    @Test
    void cashierAssignedToBranchPasses() {
        User cashier = user(1, UserRole.ROLE_BRANCH_CASHIER);
        cashier.setBranch(branch(9, store(5)));
        assertDoesNotThrow(() -> guard.requireBranchAccess(cashier, 9L));
    }

    @Test
    void cashierOfOtherBranchDenied() {
        User cashier = user(1, UserRole.ROLE_BRANCH_CASHIER);
        cashier.setBranch(branch(9, store(5)));
        assertThrows(ResourceAccessDeniedException.class,
                () -> guard.requireBranchAccess(cashier, 10L));
    }

    @Test
    void storeAdminGetsBranchOfOwnStoreViaRepository() {
        User storeAdmin = user(1, UserRole.ROLE_STORE_ADMIN);
        storeAdmin.setStore(store(5));
        when(branchRepository.findById(9L)).thenReturn(Optional.of(branch(9, store(5))));
        assertDoesNotThrow(() -> guard.requireBranchAccess(storeAdmin, 9L));
    }

    @Test
    void storeAdminGetsForeignBranchDenied() {
        User storeAdmin = user(1, UserRole.ROLE_STORE_ADMIN);
        storeAdmin.setStore(store(5));
        when(branchRepository.findById(9L)).thenReturn(Optional.of(branch(9, store(6))));
        assertThrows(ResourceAccessDeniedException.class,
                () -> guard.requireBranchAccess(storeAdmin, 9L));
    }

    // ---- requireUserAccess ----

    @Test
    void adminBypassesUserCheck() {
        User admin = user(1, UserRole.ROLE_ADMIN);
        assertDoesNotThrow(() -> guard.requireUserAccess(admin, 77L));
    }

    @Test
    void selfAccessAllowed() {
        User cashier = user(3, UserRole.ROLE_BRANCH_CASHIER);
        assertDoesNotThrow(() -> guard.requireUserAccess(cashier, 3L));
    }

    @Test
    void sameStoreUserAccessible() {
        User storeManager = user(1, UserRole.ROLE_STORE_MANAGER);
        storeManager.setStore(store(5));
        User target = user(2, UserRole.ROLE_BRANCH_CASHIER);
        target.setStore(store(5));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        assertDoesNotThrow(() -> guard.requireUserAccess(storeManager, 2L));
    }

    @Test
    void foreignStoreUserDenied() {
        User storeManager = user(1, UserRole.ROLE_STORE_MANAGER);
        storeManager.setStore(store(5));
        User target = user(2, UserRole.ROLE_BRANCH_CASHIER);
        target.setStore(store(6));
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        assertThrows(ResourceAccessDeniedException.class,
                () -> guard.requireUserAccess(storeManager, 2L));
    }

    @Test
    void missingTargetIdDeniedForNonAdmin() {
        User storeManager = user(1, UserRole.ROLE_STORE_MANAGER);
        storeManager.setStore(store(5));
        assertThrows(ResourceAccessDeniedException.class,
                () -> guard.requireUserAccess(storeManager, null));
    }
}
