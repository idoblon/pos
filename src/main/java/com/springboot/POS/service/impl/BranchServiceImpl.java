package com.springboot.POS.service.impl;

import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.BranchMapper;
import com.springboot.POS.modal.Branch;
import com.springboot.POS.modal.Store;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.BranchDTO;
import com.springboot.POS.repository.BranchRepository;
import com.springboot.POS.repository.StoreRepository;
import com.springboot.POS.repository.UserRepository;
import com.springboot.POS.service.BranchService;
import com.springboot.POS.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;


    public BranchServiceImpl(BranchRepository branchRepository, StoreRepository storeRepository, UserRepository userRepository, UserService userService) {
        this.branchRepository = branchRepository;
        this.storeRepository = storeRepository;
        this.userService = userService;
    }

    @Override
    public BranchDTO createBranch(BranchDTO branchDTO) throws UserException {
        User currentUser = userService.getCurrentUser();
        Store store = storeRepository.findByStoreAdminId(currentUser.getId());

        Branch branch = BranchMapper.toEntity(branchDTO, store);
        Branch savedBranch = branchRepository.save(branch);

        return BranchMapper.toDTO(savedBranch);
    }

    @Override
    public BranchDTO updateBranch(Long id, BranchDTO branchDTO) throws Exception {
        Branch existing = branchRepository.findById(id).orElseThrow(
                ()-> new Exception("branch does not exist....")
        );

        existing.setName(branchDTO.getName());
        existing.setWorkingDays(branchDTO.getWorkingDays());
        existing.setEmail(branchDTO.getEmail());
        existing.setPhone(branchDTO.getPhone());
        existing.setAddress(branchDTO.getAddress());
        existing.setOpenTime(branchDTO.getOpenTime());
        existing.setCloseTime(branchDTO.getCloseTime());
        existing.setUpdatedAt(LocalDateTime.now());

        Branch updtedBranch = branchRepository.save(existing);
        return BranchMapper.toDTO(updtedBranch);
    }

    @Override
    public void  deleteBranch(Long id) throws Exception {
        Branch existing = branchRepository.findById(id).orElseThrow(
                ()-> new Exception("branch does not exist....")
        );
        branchRepository.delete(existing);
    }

    @Override
    public List<BranchDTO> getAllBranchesByStoreId(Long storeId) {
        List<Branch> branches = branchRepository.findByStoreId(storeId);
        return branches.stream().map(BranchMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public BranchDTO getBranchById(Long id) throws Exception {
        Branch existing = branchRepository.findById(id).orElseThrow(
                ()-> new Exception("branch does not exist....")
        );
        return BranchMapper.toDTO(existing);
    }
}
