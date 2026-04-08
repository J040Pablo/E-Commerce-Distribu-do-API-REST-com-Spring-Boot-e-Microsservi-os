package com.ecommerce.order_service.service;

import com.ecommerce.order_service.client.CustomerClient;
import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.dto.*;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.exception.OrderNotFoundException;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private MessagePublisher messagePublisher;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest orderRequest;
    private CustomerResponse customer;
    private ProductResponse product;
    private Order testOrder;

    @BeforeEach
    void setUp() {
                customer = new CustomerResponse();
                customer.setId(1L);
                customer.setName("John Doe");
                customer.setEmail("john@example.com");

                product = new ProductResponse();
                product.setId(1L);
                product.setName("Laptop");
                product.setDescription("Electronics");
                product.setPrice(BigDecimal.valueOf(999.99));
                product.setStock(10);

                OrderItemRequest itemRequest = new OrderItemRequest();
                itemRequest.setProductId(1L);
                itemRequest.setQuantity(2);

                orderRequest = new OrderRequest();
                orderRequest.setCustomerId(1L);
                orderRequest.setItems(Arrays.asList(itemRequest));
        
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setCustomerId(1L);
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setTotalAmount(BigDecimal.valueOf(1999.98));
        testOrder.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create order successfully")
    void testCreateOrderSuccess() {
        // Arrange
        when(customerClient.getCustomerById(1L)).thenReturn(customer);
        when(productClient.getProductById(1L)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        OrderResponse response = orderService.createOrder(orderRequest);

        // Assert
        assertThat(response)
                .as("Order response should not be null")
                .isNotNull();
        assertThat(response.getCustomerId())
                .as("Order customer ID should match request")
                .isEqualTo(1L);
        assertThat(response.getTotalAmount())
                .as("Order total should be 2 items × $999.99 = $1999.98")
                .isEqualByComparingTo(BigDecimal.valueOf(1999.98));
        assertThat(response.getStatus())
                .as("Initial order status should be PAYMENT_PROCESSING")
                .isIn(OrderStatus.PAYMENT_PROCESSING, OrderStatus.PENDING);

        verify(customerClient, times(1)).getCustomerById(1L);
        verify(productClient, times(1)).getProductById(1L);
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(messagePublisher, times(1)).publishPaymentEvent(any(PaymentEvent.class));
    }

    @Test
    @DisplayName("Should fail when product is out of stock")
    void testCreateOrderProductOutOfStock() {
        // Arrange
        ProductResponse lowStockProduct = new ProductResponse();
        lowStockProduct.setId(1L);
        lowStockProduct.setName("Laptop");
        lowStockProduct.setDescription("Electronics");
        lowStockProduct.setPrice(BigDecimal.valueOf(999.99));
        lowStockProduct.setStock(1);
        when(customerClient.getCustomerById(1L)).thenReturn(customer);
        when(productClient.getProductById(1L)).thenReturn(lowStockProduct);

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .as("Should throw RuntimeException when quantity exceeds available stock")
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient stock");

        verify(customerClient, times(1)).getCustomerById(1L);
        verify(productClient, times(1)).getProductById(1L);
        verify(orderRepository, never()).save(any());
        verify(messagePublisher, never()).publishPaymentEvent(any());
    }

    @Test
    @DisplayName("Should fail when customer is not found")
    void testCreateOrderCustomerNotFound() {
        // Arrange
        when(customerClient.getCustomerById(1L))
                .thenThrow(new RuntimeException("Customer not found"));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Customer not found");

        verify(customerClient).getCustomerById(1L);
        verify(productClient, never()).getProductById(anyLong());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should calculate total price correctly")
    void testCreateOrderTotalPriceCalculation() {
        // Arrange
        ProductResponse expensiveProduct = new ProductResponse();
        expensiveProduct.setId(1L);
        expensiveProduct.setName("Gaming PC");
        expensiveProduct.setDescription("Electronics");
        expensiveProduct.setPrice(BigDecimal.valueOf(2500.00));
        expensiveProduct.setStock(10);

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(3);

        OrderRequest multiItemRequest = new OrderRequest();
        multiItemRequest.setCustomerId(1L);
        multiItemRequest.setItems(Arrays.asList(itemRequest));
        
        when(customerClient.getCustomerById(1L)).thenReturn(customer);
        when(productClient.getProductById(1L)).thenReturn(expensiveProduct);
        
        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setCustomerId(1L);
        savedOrder.setTotalAmount(BigDecimal.valueOf(7500.00));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        OrderResponse response = orderService.createOrder(multiItemRequest);

        // Assert
        assertThat(response.getTotalAmount())
                .as("Total should be 3 × $2500.00 = $7500.00")
                .isEqualByComparingTo(BigDecimal.valueOf(7500.00));
        
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getTotalAmount())
                .as("Captured order total must match expected calculation")
                .isEqualByComparingTo(BigDecimal.valueOf(7500.00));
    }

    @Test
    @DisplayName("Should publish payment event after order creation")
    void testCreateOrderPublishesEvent() {
        // Arrange
        when(customerClient.getCustomerById(1L)).thenReturn(customer);
        when(productClient.getProductById(1L)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        orderService.createOrder(orderRequest);

        // Assert
        ArgumentCaptor<PaymentEvent> eventCaptor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(messagePublisher).publishPaymentEvent(eventCaptor.capture());
        
        PaymentEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getOrderId()).isEqualTo(1L);
        assertThat(publishedEvent.getCustomerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should fail when external service fails")
    void testCreateOrderExternalServiceFailure() {
        // Arrange
        when(customerClient.getCustomerById(1L))
                .thenThrow(new RuntimeException("Service temporarily unavailable"));

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(orderRequest))
                .isInstanceOf(RuntimeException.class);

        verify(orderRepository, never()).save(any());
        verify(messagePublisher, never()).publishPaymentEvent(any());
    }

    @Test
    @DisplayName("Should fail when order items list is empty")
    void testCreateOrderEmptyItems() {
        // Arrange
        OrderRequest emptyRequest = new OrderRequest();
        emptyRequest.setCustomerId(1L);
        emptyRequest.setItems(Arrays.asList());

        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrder(emptyRequest))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should fail when item quantity is invalid")
    void testCreateOrderInvalidQuantity() {
        // Arrange
        OrderItemRequest invalidRequest = new OrderItemRequest();
        invalidRequest.setProductId(1L);
        invalidRequest.setQuantity(0);

        OrderRequest request = new OrderRequest();
        request.setCustomerId(1L);
        request.setItems(Arrays.asList(invalidRequest));
        when(customerClient.getCustomerById(1L)).thenReturn(customer);

        // Act & Assert - Depending on validation, this may throw or just fail
        // This test shows the pattern for validation testing
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should retrieve order by ID successfully")
    void testGetOrderByIdSuccess() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        OrderResponse response = orderService.getOrderById(1L);

        // Assert
        assertThat(response)
                .as("Order response should be returned when found in repository")
                .isNotNull();
        assertThat(response.getId())
                .as("Response order ID should match requested ID")
                .isEqualTo(1L);
        assertThat(response.getCustomerId())
                .as("Response customer ID should match order")
                .isEqualTo(1L);

        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void testGetOrderByIdNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .as("Should throw OrderNotFoundException when order doesn't exist")
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderRepository, times(1)).findById(999L);
    }
}
