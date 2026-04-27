package com.springboot.POS.service;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.BranchDTO;

import java.util.List;

public interface BranchService {

    BranchDTO createBranch(BranchDTO branchDTO) throws UserException;
    BranchDTO updateBranch(Long id, BranchDTO branchDTO) throws Exception;
    void deleteBranch(Long id) throws Exception;
    List<BranchDTO> getAllBranchesByStoreId(Long storeId);
    BranchDTO getBranchById(Long id) throws Exception;
}
