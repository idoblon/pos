package com.springboot.POS.service.impl;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.User;
import com.springboot.POS.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OwnershipGuard {

    private final BranchRepository branchRepository;

    // Throws if the user does not belong to the given store
    public void requireStoreAccess(User user, Long storeId) throws UserException {
        if (user.getRole() == UserRole.ROLE_ADMIN) return;
        Long userStoreId = resolveStoreId(user);
        if (userStoreId == null || !userStoreId.equals(storeId)) {
            throw new UserException("Access denied: resource does not belong to your store");
        }
    }

    // Throws if the user does not belong to the given branch
    public void requireBranchAccess(User user, Long branchId) throws UserException {
        if (user.getRole() == UserRole.ROLE_ADMIN) return;
        if (user.getRole() == UserRole.ROLE_STORE_ADMIN || user.getRole() == UserRole.ROLE_STORE_MANAGER) {
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new UserException("Branch not found"));
            if (!branch.getStore().getId().equals(resolveStoreId(user))) {
                throw new UserException("Access denied: branch does not belong to your store");
            }
            return;
        }
        if (user.getBranch() == null || !user.getBranch().getId().equals(branchId)) {
            throw new UserException("Access denied: you are not assigned to this branch");
        }
    }

    private Long resolveStoreId(User user) {
        if (user.getStore() != null) return user.getStore().getId();
        if (user.getBranch() != null && user.getBranch().getStore() != null)
            return user.getBranch().getStore().getId();
        return null;
    }
}
