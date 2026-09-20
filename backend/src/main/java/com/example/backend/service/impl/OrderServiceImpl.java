package com.example.backend.service.impl;

import com.example.backend.constant.ApplicationConstants;
import com.example.backend.dto.OrderItemResponseDto;
import com.example.backend.dto.OrderResponseDto;
import com.example.backend.entity.Customer;
import com.example.backend.entity.Order;
import com.example.backend.entity.OrderItem;
import com.example.backend.entity.Product;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.payload.OrderRequestPayload;
import com.example.backend.repository.OrderRepo;
import com.example.backend.repository.ProductRepo;
import com.example.backend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepo orderRepo;
    private final ProductRepo productRepo;
    private final ProfileServiceImpl profileServiceImpl;

    /**
     * CheckoutForm.jsx 送出 payment 後，建立訂單
     */
    @Override
    public void createOrder(OrderRequestPayload orderRequestPayload) {
        // 1. 取得當前登入用戶的 Customer 物件
        Customer customer = profileServiceImpl.getAuthenticatedCustomer();

        // 2. 建立 Order Entity 物件
        Order order = new Order();
        order.setCustomer(customer); // * 設定 Order 所属的 Customer -> 必要 (關係的「真正擁有方」是 Order 這邊)，負責 FK CUSTOMER_ID
        BeanUtils.copyProperties(orderRequestPayload, order); // 只複製外層且型別相容的欄位（totalPrice, paymentId, paymentStatus）；orderItems 需手動轉成 OrderItem entity
        order.setOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED); // 訂單建立 CREATED
        // 2.1 遍歷 orderItems 並建立 OrderItem Entity 物件
        List<OrderItem> orderItems = orderRequestPayload.orderItems().stream()
                .map(item -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order); // * 設定 OrderItem 所属的 Order -> 必要 (關係的「真正擁有方」是 OrderItem 這邊)，負責 FK ORDER_ID
                    Product product = productRepo.findById(item.productId()) // 取得 Product Optional
                            .orElseThrow(() -> new ResourceNotFoundException("Product", "ProductID",
                                    item.productId().toString())); // 如果找不到 Product 則拋出 ResourceNotFoundException: 找不到符合條件的 Product，欄位 ProductID 的值為 item.productId()
                    orderItem.setProduct(product); // * 設定 OrderItem 所属的 Product -> 必要 (關係的「真正擁有方」是 OrderItem 這邊)，負責 FK PRODUCT_ID
                    orderItem.setQuantity(item.quantity());
                    orderItem.setPrice(item.price());
                    return orderItem;
                }).collect(Collectors.toList()); // 將 Stream<OrderItem> 轉換成 List<OrderItem>
        order.setOrderItems(orderItems); // 設定 Order 持有的 OrderItems，讓 cascade save 可以一起儲存訂單明細

        //3. 儲存 Order Entity 物件，包含所有的 OrderItem
        orderRepo.save(order);
    }

    /**
     * 取得當前用戶的所有訂單（購買紀錄）
     */
    @Override
    @Transactional(readOnly = true) // 讓 mapToOrderResponseDTO 存取 LAZY 的 order.orderItems 時 session 仍開著
    public List<OrderResponseDto> getCustomerOrders() {
        // 1. 取得當前登入用戶的 Customer 物件
        Customer customer = profileServiceImpl.getAuthenticatedCustomer();

        // 2. 取得 Customer 的所有訂單
        List<Order> orders = orderRepo.findByCustomerOrderByCreatedAtDesc(customer);

        // 3. 將 Order 列表轉換成 OrderResponseDto 列表
        return orders.stream().map(this::mapToOrderResponseDTO).collect(Collectors.toList());
    }

    /**
     * for Admin use
     * 取得所有狀態為 CREATED 的訂單
     */
    @Override
    @Transactional(readOnly = true) // 同上；private 的 mapToOrderResponseDTO 無法自行標註（Spring AOP 不攔 private／自我呼叫）
    public List<OrderResponseDto> getAllPendingOrders() {
        // 1. 取得所有狀態為 CREATED 的訂單
        List<Order> orders = orderRepo.findByOrderStatus(ApplicationConstants.ORDER_STATUS_CREATED);
        // 2. 將 Order 列表轉換成 OrderResponseDto 列表
        return orders.stream().map(this::mapToOrderResponseDTO).collect(Collectors.toList());
    }

    /**
     * for Admin use
     * 更新訂單狀態 CONFIRMED or CANCELLED
     */
    @Override
    public Order updateOrderStatus(Long orderId, String orderStatus) {
        // 1. 取得指定 ID 的訂單，若找不到則拋出 ResourceNotFoundException　找不到符合條件的 %s，欄位 %s 的值為 '%s'
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "OrderID", orderId.toString()));

        // 2. 更新訂單狀態
        order.setOrderStatus(orderStatus);
        // 3. 儲存更新後的訂單，返回更新後的訂單物件
        return orderRepo.save(order);
    }


    /**
     * Helper method
     * 將 Order 轉換成 OrderResponseDto
     */
    private OrderResponseDto mapToOrderResponseDTO(Order order) {
        // 1. 取得 Order 的所有 OrderItem
        List<OrderItemResponseDto> orderItemsDTOs = order.getOrderItems().stream()
                .map(this::mapToOrderItemResponseDTO)
                .collect(Collectors.toList());
        // 2. 建立 OrderResponseDto 物件
        OrderResponseDto orderResponseDto = new OrderResponseDto(
                order.getId(),
                order.getOrderStatus(),
                order.getTotalPrice(),
                order.getCreatedAt().toString(),
                orderItemsDTOs);

        return orderResponseDto;
    }

    /**
     * Helper method
     * 將 OrderItem 轉換成 OrderItemResponseDto
     */
    private OrderItemResponseDto mapToOrderItemResponseDTO(OrderItem orderItem) {
        // 1. 取得 OrderItem 的 Product 名稱、數量、價格及圖片 URL
        OrderItemResponseDto orderItemDto = new OrderItemResponseDto(
                orderItem.getProduct().getName(),
                orderItem.getQuantity(),
                orderItem.getPrice(),
                orderItem.getProduct().getImageUrl());

        return orderItemDto;
    }
}
