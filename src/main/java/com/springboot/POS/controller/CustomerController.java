package com.springboot.POS.controller;

import com.springboot.POS.domain.UserRole;
import com.springboot.POS.exceptions.UserException;
import com.springboot.POS.modal.Customer;
import com.springboot.POS.modal.User;
import com.springboot.POS.payload.response.ApiResponse;
import com.springboot.POS.service.CustomerService;
import com.springboot.POS.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final UserService userService;

    private User getUser(String jwt) throws UserException {
        return userService.getUserFromJwtToken(jwt);
    }

    private Long getStoreId(User user) throws UserException {
        if (user.getRole() == UserRole.ROLE_ADMIN) return null; // admin sees all
        Long storeId = user.getStoreId();
        if (storeId == null && user.getBranch() != null && user.getBranch().getStore() != null) {
            storeId = user.getBranch().getStore().getId();
        }
        if (storeId == null) throw new UserException("Unable to resolve store from token");
        return storeId;
    }

    @PostMapping
    public ResponseEntity<Customer> create(
            @RequestBody Customer customer,
            @RequestHeader("Authorization") String jwt) throws UserException {
        User user = getUser(jwt);
        Long storeId = getStoreId(user);
        if (storeId == null) throw new UserException("Admin cannot create customers on behalf of a store");
        return ResponseEntity.ok(customerService.createCustomer(customer, storeId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> update(
            @PathVariable Long id,
            @RequestBody Customer customer,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = getUser(jwt);
        Long storeId = getStoreId(user);
        if (storeId == null) throw new UserException("Admin cannot update store customers directly");
        return ResponseEntity.ok(customerService.updateCustomer(id, customer, storeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = getUser(jwt);
        Long storeId = getStoreId(user);
        if (storeId == null) throw new UserException("Admin cannot delete store customers directly");
        customerService.deleteCustomer(id, storeId);
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Customer deleted");
        return ResponseEntity.ok(apiResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = getUser(jwt);
        Long storeId = getStoreId(user);
        // Admin can fetch any customer by id without store restriction
        return ResponseEntity.ok(storeId == null
                ? customerService.getCustomerById(id)
                : customerService.getCustomer(id, storeId));
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAll(
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = getUser(jwt);
        Long storeId = getStoreId(user);
        // Admin gets all customers across all stores
        return ResponseEntity.ok(storeId == null
                ? customerService.getAllCustomers()
                : customerService.getAllCustomers(storeId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Customer>> search(
            @RequestParam String q,
            @RequestHeader("Authorization") String jwt) throws Exception {
        User user = getUser(jwt);
        Long storeId = getStoreId(user);
        return ResponseEntity.ok(storeId == null
                ? customerService.searchCustomers(q)
                : customerService.searchCustomers(q, storeId));
    }
}
