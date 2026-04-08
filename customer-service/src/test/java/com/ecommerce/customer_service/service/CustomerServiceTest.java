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

import static org.assertj.core.api.Assertions.*;
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
        assertThat(response)
                .as("Customer response should not be null")
                .isNotNull();
        assertThat(response.getId())
                .as("Response ID should match saved customer")
                .isEqualTo(customer.getId());
        assertThat(response.getName())
                .as("Response name should match request")
                .isEqualTo(customer.getName());
        assertThat(response.getEmail())
                .as("Response email should match request")
                .isEqualTo(customer.getEmail());
        
        verify(customerRepository, times(1)).existsByEmail(customerRequest.getEmail());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void createCustomer_EmailAlreadyExists_ThrowsException() {
        // Arrange
        when(customerRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> customerService.createCustomer(customerRequest))
            .as("Should throw CustomerAlreadyExistsException when email exists")
            .isInstanceOf(CustomerAlreadyExistsException.class);
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
        assertThat(response)
                .as("Customer response should not be null")
                .isNotNull();
        assertThat(response.getId())
            .as("Response ID should match saved customer")
            .isEqualTo(customer.getId());
        assertThat(response.getEmail())
            .as("Response email should match request")
            .isEqualTo(customer.getEmail());
        verify(customerRepository).findById(1L);
    }

    @Test
    void getCustomerById_NotFound_ThrowsException() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.getCustomerById(1L))
            .as("Should throw CustomerNotFoundException when customer not found")
            .isInstanceOf(CustomerNotFoundException.class);
        verify(customerRepository).findById(1L);
    }

    @Test
    void getCustomerByEmail_Success() {
        // Arrange
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(customer));

        // Act
        CustomerResponse response = customerService.getCustomerByEmail("joao@example.com");

        // Assert
        assertThat(response)
            .as("Customer response should not be null")
            .isNotNull();
        assertThat(response.getEmail())
            .as("Response email should match saved customer")
            .isEqualTo(customer.getEmail());
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
        assertThat(responses)
            .as("Customers list should not be null")
            .isNotNull()
            .hasSize(2);
        verify(customerRepository).findAll();
    }

    @Test
    void updateCustomer_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        customerRequest.setName("João Silva Updated");
        customerRequest.setEmail("joao@example.com");

        // Act
        CustomerResponse response = customerService.updateCustomer(1L, customerRequest);

        // Assert
        assertThat(response)
            .as("Customer response should not be null")
            .isNotNull();
        assertThat(response.getName())
            .as("Response name should be updated")
            .isEqualTo("João Silva Updated");
        assertThat(response.getEmail())
            .as("Response email should remain the same")
            .isEqualTo("joao@example.com");
        verify(customerRepository).findById(1L);
        verify(customerRepository).save(any(Customer.class));
        verify(customerRepository, never()).existsByEmail(anyString());
    }

    @Test
    void updateCustomer_EmailAlreadyInUse_ThrowsException() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        customerRequest.setEmail("outro@example.com");
        when(customerRepository.existsByEmail("outro@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> customerService.updateCustomer(1L, customerRequest))
            .as("Should throw CustomerAlreadyExistsException when email already in use")
            .isInstanceOf(CustomerAlreadyExistsException.class);
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
        assertThatThrownBy(() -> customerService.deleteCustomer(1L))
            .as("Should throw CustomerNotFoundException when customer not found")
            .isInstanceOf(CustomerNotFoundException.class);
        verify(customerRepository).existsById(1L);
        verify(customerRepository, never()).deleteById(any());
    }
}
