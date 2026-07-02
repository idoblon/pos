package com.springboot.POS.service;

import com.springboot.POS.modal.Customer;

import java.util.List;

public interface CustomerService {

    // Store-scoped operations
    Customer createCustomer(Customer customer, Long storeId);
    Customer updateCustomer(Long id, Customer customer, Long storeId) throws Exception;
    void deleteCustomer(Long id, Long storeId) throws Exception;
    Customer getCustomer(Long id, Long storeId) throws Exception;
    List<Customer> getAllCustomers(Long storeId) throws Exception;
    List<Customer> searchCustomers(String keyword, Long storeId);

    // Admin-only unscoped operations
    Customer getCustomerById(Long id) throws Exception;
    List<Customer> getAllCustomers() throws Exception;
    List<Customer> searchCustomers(String keyword);
}
