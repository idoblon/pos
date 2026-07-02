package com.springboot.POS.service.impl;

import com.springboot.POS.modal.Customer;
import com.springboot.POS.repository.CustomerRepository;
import com.springboot.POS.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public Customer createCustomer(Customer customer, Long storeId) {
        customer.setStoreId(storeId);
        return customerRepository.save(customer);
    }

    @Override
    public Customer updateCustomer(Long id, Customer customer, Long storeId) throws Exception {
        Customer existing = customerRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new Exception("Customer not found or does not belong to your store"));
        existing.setFullName(customer.getFullName());
        existing.setEmail(customer.getEmail());
        existing.setPhone(customer.getPhone());
        existing.setAddress(customer.getAddress());
        return customerRepository.save(existing);
    }

    @Override
    public void deleteCustomer(Long id, Long storeId) throws Exception {
        Customer customer = customerRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new Exception("Customer not found or does not belong to your store"));
        customerRepository.delete(customer);
    }

    @Override
    public Customer getCustomer(Long id, Long storeId) throws Exception {
        Customer customer = customerRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new Exception("Customer not found or does not belong to your store"));
        customer.setTotalOrders(customerRepository.countOrdersByCustomerId(id));
        return customer;
    }

    @Override
    public List<Customer> getAllCustomers(Long storeId) throws Exception {
        return customerRepository.findByStoreId(storeId).stream().map(c -> {
            c.setTotalOrders(customerRepository.countOrdersByCustomerId(c.getId()));
            return c;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Customer> searchCustomers(String keyword, Long storeId) {
        List<Customer> nameMatches = customerRepository
                .findByStoreIdAndFullNameContainingIgnoreCaseOrderByFullName(storeId, keyword);
        List<Customer> emailMatches = customerRepository
                .findByStoreIdAndEmailContainingIgnoreCaseOrderByFullName(storeId, keyword);

        emailMatches.removeIf(c -> nameMatches.stream().anyMatch(n -> n.getId().equals(c.getId())));
        nameMatches.addAll(emailMatches);
        return nameMatches;
    }

    // Admin-only: unscoped access across all stores
    @Override
    public Customer getCustomerById(Long id) throws Exception {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new Exception("Customer not found"));
        customer.setTotalOrders(customerRepository.countOrdersByCustomerId(id));
        return customer;
    }

    @Override
    public List<Customer> getAllCustomers() throws Exception {
        return customerRepository.findAll().stream().map(c -> {
            c.setTotalOrders(customerRepository.countOrdersByCustomerId(c.getId()));
            return c;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Customer> searchCustomers(String keyword) {
        List<Customer> nameMatches = customerRepository
                .findByFullNameContainingIgnoreCaseOrderByFullName(keyword);
        List<Customer> emailMatches = customerRepository
                .findByEmailContainingIgnoreCaseOrderByFullName(keyword);
        emailMatches.removeIf(c -> nameMatches.stream().anyMatch(n -> n.getId().equals(c.getId())));
        nameMatches.addAll(emailMatches);
        return nameMatches;
    }
}
