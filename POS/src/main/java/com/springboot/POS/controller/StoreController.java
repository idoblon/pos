package com.springboot.POS.controller;

import com.springboot.POS.domain.StoreStatus;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.mapper.StoreMapper;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.dto.StoreDTO;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.StoreService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/store")
public class StoreController {

    private final StoreService storeService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<StoreDTO> createStore(@RequestBody StoreDTO storeDTO,
                                                @RequestHeader("Authorization")String jwt) throws UserException {
        User user = userService.getUserFromJwtToken(jwt);
        return ResponseEntity.ok(storeService.createStore(storeDTO, user));

    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDTO> getStoreById(@PathVariable Long id ,
                                                @RequestHeader("Authorization")String jwt) throws Exception {
        User user = userService.getUserFromJwtToken(jwt);

        return ResponseEntity.ok(storeService.getStoreById(id));

    }


    @GetMapping()
    public ResponseEntity<List<StoreDTO>> getAllStores(
            @RequestHeader("Authorization")String jwt) throws Exception {

        return ResponseEntity.ok(storeService.getAllStores());

    }

    @GetMapping("/admin")
    public ResponseEntity<StoreDTO> getStoreByAdmin(
            @RequestHeader("Authorization")String jwt) throws Exception {

        return ResponseEntity.ok(StoreMapper.toDTO(storeService.getStoreByAdmin()));

    }

    @GetMapping("/employee")
    public ResponseEntity<StoreDTO> getStoreByEmployee(
            @RequestHeader("Authorization")String jwt) throws Exception {

        return ResponseEntity.ok(storeService.getStoreByEmployee());

    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreDTO> updateStore(@PathVariable Long id,
                                                @RequestBody StoreDTO storeDTO) throws Exception{
        return ResponseEntity.ok(storeService.updateStore(id, storeDTO));
    }

    @PutMapping("/{id}/moderate")
    public ResponseEntity<StoreDTO> moderateStore(@PathVariable Long id,
                                                @RequestParam StoreStatus status,
                                                @RequestBody StoreDTO storeDTO) throws Exception{
        return ResponseEntity.ok(storeService.moderateStore(id, status));
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteStore(@PathVariable Long id)
                                                  throws Exception{
        storeService.deleteStore(id);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Store deleted successfully");
        return ResponseEntity.ok(apiResponse);
    }
}
