package com.springboot.POS.service.impl;

import com.springboot.POS.modal.Customer;
import com.springboot.POS.repository.CustomerRepository;
import com.springboot.POS.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.ExpressionException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    public Customer updateCustomer(Long id, Customer customer) throws Exception {
        Customer existing = customerRepository.findById(id).orElseThrow(
                () -> new Exception("Customer not found")
        );
        existing.setFullName(customer.getFullName());
        existing.setEmail(customer.getEmail());
        existing.setPhone(customer.getPhone());
        existing.setAddress(customer.getAddress());
        return customerRepository.save(existing);
    }

    @Override
    public void deleteCustomer(Long id) throws Exception {
        Customer customerToUpdate = customerRepository.findById(id).orElseThrow(
                () -> new Exception("Customer not found")
        );
        customerRepository.delete(customerToUpdate);
    }

    @Override
    public Customer getCustomer(Long id) throws Exception {
        Customer customer = customerRepository.findById(id).orElseThrow(
                () -> new Exception("Customer not found")
        );
        customer.setTotalOrders(customerRepository.countOrdersByCustomerId(id));
        return customer;
    }

    @Override
    public List<Customer> getAllCustomers() throws Exception {
        return customerRepository.findAll().stream().map(c -> {
            c.setTotalOrders(customerRepository.countOrdersByCustomerId(c.getId()));
            return c;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Customer> searchCustomers(String keyword) {
        // First get customers matching by fullName
        List<Customer> nameMatches = customerRepository.findByFullNameContainingIgnoreCaseOrderByFullName(keyword);
        
        // Then get customers matching by email (excluding those already found by name)
        List<Customer> emailMatches = customerRepository.findByEmailContainingIgnoreCaseOrderByFullName(keyword);
        
        // Remove duplicates and combine results (name matches first)
        emailMatches.removeIf(customer -> nameMatches.stream()
                .anyMatch(nameMatch -> nameMatch.getId().equals(customer.getId())));
        
        nameMatches.addAll(emailMatches);
        return nameMatches;
    }
}
