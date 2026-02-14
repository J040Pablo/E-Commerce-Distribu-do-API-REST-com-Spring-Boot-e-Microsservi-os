package com.ecommerce.customer_service.service;

import com.ecommerce.customer_service.dto.CustomerRequest;
import com.ecommerce.customer_service.dto.CustomerResponse;
import com.ecommerce.customer_service.entity.Customer;
import com.ecommerce.customer_service.exception.CustomerAlreadyExistsException;
import com.ecommerce.customer_service.exception.CustomerNotFoundException;
import com.ecommerce.customer_service.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private CustomerRequest customerRequest;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setName("João Silva");
        customer.setEmail("joao@example.com");
        customer.setPhone("11999999999");
        customer.setAddress("Rua ABC, 123");
        customer.setCity("São Paulo");
        customer.setState("SP");
        customer.setZipCode("01234-567");
        customer.setCreatedAt(LocalDateTime.now());

        customerRequest = new CustomerRequest();
        customerRequest.setName("João Silva");
        customerRequest.setEmail("joao@example.com");
        customerRequest.setPhone("11999999999");
        customerRequest.setAddress("Rua ABC, 123");
        customerRequest.setCity("São Paulo");
        customerRequest.setState("SP");
        customerRequest.setZipCode("01234-567");
    }

    @Test
    void createCustomer_Success() {
        // Arrange
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // Act
        CustomerResponse response = customerService.createCustomer(customerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(customer.getId(), response.getId());
        assertEquals(customer.getName(), response.getName());
        assertEquals(customer.getEmail(), response.getEmail());
        verify(customerRepository).existsByEmail(customerRequest.getEmail());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void createCustomer_EmailAlreadyExists_ThrowsException() {
        // Arrange
        when(customerRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(CustomerAlreadyExistsException.class, () -> 
            customerService.createCustomer(customerRequest));
        verify(customerRepository).existsByEmail(customerRequest.getEmail());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void getCustomerById_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act
        CustomerResponse response = customerService.getCustomerById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(customer.getId(), response.getId());
        assertEquals(customer.getEmail(), response.getEmail());
        verify(customerRepository).findById(1L);
    }

    @Test
    void getCustomerById_NotFound_ThrowsException() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> 
            customerService.getCustomerById(1L));
        verify(customerRepository).findById(1L);
    }

    @Test
    void getCustomerByEmail_Success() {
        // Arrange
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(customer));

        // Act
        CustomerResponse response = customerService.getCustomerByEmail("joao@example.com");

        // Assert
        assertNotNull(response);
        assertEquals(customer.getEmail(), response.getEmail());
        verify(customerRepository).findByEmail("joao@example.com");
    }

    @Test
    void getAllCustomers_Success() {
        // Arrange
        Customer customer2 = new Customer();
        customer2.setId(2L);
        customer2.setName("Maria Santos");
        customer2.setEmail("maria@example.com");
        
        when(customerRepository.findAll()).thenReturn(Arrays.asList(customer, customer2));

        // Act
        List<CustomerResponse> responses = customerService.getAllCustomers();

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(customerRepository).findAll();
    }

    @Test
    void updateCustomer_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        customerRequest.setName("João Silva Updated");

        // Act
        CustomerResponse response = customerService.updateCustomer(1L, customerRequest);

        // Assert
        assertNotNull(response);
        verify(customerRepository).findById(1L);
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void updateCustomer_EmailAlreadyInUse_ThrowsException() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        customerRequest.setEmail("outro@example.com");
        when(customerRepository.existsByEmail("outro@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(CustomerAlreadyExistsException.class, () -> 
            customerService.updateCustomer(1L, customerRequest));
        verify(customerRepository).findById(1L);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void deleteCustomer_Success() {
        // Arrange
        when(customerRepository.existsById(1L)).thenReturn(true);

        // Act
        customerService.deleteCustomer(1L);

        // Assert
        verify(customerRepository).existsById(1L);
        verify(customerRepository).deleteById(1L);
    }

    @Test
    void deleteCustomer_NotFound_ThrowsException() {
        // Arrange
        when(customerRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> 
            customerService.deleteCustomer(1L));
        verify(customerRepository).existsById(1L);
        verify(customerRepository, never()).deleteById(any());
    }
}
