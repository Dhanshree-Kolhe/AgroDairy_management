package com.agrodairy.management.service.impl;

import com.agrodairy.management.dao.CustomerDAO;
import com.agrodairy.management.dao.ExtraOrderDAO;
import com.agrodairy.management.dao.ProductDAO;
import com.agrodairy.management.dao.impl.CustomerDAOImpl;
import com.agrodairy.management.dao.impl.ExtraOrderDAOImpl;
import com.agrodairy.management.dao.impl.ProductDAOImpl;
import com.agrodairy.management.exception.ResourceNotFoundException;
import com.agrodairy.management.exception.ValidationException;
import com.agrodairy.management.model.ExtraOrder;
import com.agrodairy.management.model.Product;
import com.agrodairy.management.model.enums.RequestStatus;
import com.agrodairy.management.service.ExtraOrderService;
import com.agrodairy.management.service.InventoryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.List;

public class ExtraOrderServiceImpl implements ExtraOrderService {
    private final ExtraOrderDAO extraOrderDAO;
    private final ProductDAO productDAO;
    private final CustomerDAO customerDAO;
    private final InventoryService inventoryService;

    public ExtraOrderServiceImpl() {
        this.extraOrderDAO = new ExtraOrderDAOImpl();
        this.productDAO = new ProductDAOImpl();
        this.customerDAO = new CustomerDAOImpl();
        this.inventoryService = new InventoryServiceImpl();
    }

    public ExtraOrderServiceImpl(ExtraOrderDAO extraOrderDAO, ProductDAO productDAO, CustomerDAO customerDAO, InventoryService inventoryService) {
        this.extraOrderDAO = extraOrderDAO;
        this.productDAO = productDAO;
        this.customerDAO = customerDAO;
        this.inventoryService = inventoryService;
    }

    @Override
    public ExtraOrder createExtraOrder(Long customerId, Long productId, BigDecimal quantity, Date deliveryDate) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Customer ID is required.");
        if (productId == null || productId <= 0) throw new ValidationException("Product ID is required.");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Order quantity must be greater than zero.");
        }
        if (deliveryDate == null) throw new ValidationException("Delivery date is required.");

        customerDAO.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        // Preserve Price Snapshot at order creation time
        BigDecimal priceSnapshot = product.getPrice();
        BigDecimal totalPrice = quantity.multiply(priceSnapshot).setScale(2, RoundingMode.HALF_UP);

        ExtraOrder order = new ExtraOrder();
        order.setCustomerId(customerId);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setPriceSnapshot(priceSnapshot); // PRICE SNAPSHOT PRESERVATION
        order.setTotalPrice(totalPrice);
        order.setOrderDate(new Date(System.currentTimeMillis()));
        order.setDeliveryDate(deliveryDate);
        order.setStatus(RequestStatus.PENDING);

        Long id = extraOrderDAO.insert(order);
        order.setOrderId(id);
        return order;
    }

    @Override
    public ExtraOrder approveOrder(Long orderId) {
        ExtraOrder order = getOrderById(orderId);
        if (order.getStatus() == RequestStatus.APPROVED || order.getStatus() == RequestStatus.DELIVERED) {
            return order; // Already approved
        }

        // Deduct inventory quantity upon approval
        inventoryService.reduceStock(order.getProductId(), order.getQuantity());

        extraOrderDAO.updateStatus(orderId, RequestStatus.APPROVED);
        order.setStatus(RequestStatus.APPROVED);
        return order;
    }

    @Override
    public ExtraOrder rejectOrder(Long orderId) {
        ExtraOrder order = getOrderById(orderId);
        extraOrderDAO.updateStatus(orderId, RequestStatus.REJECTED);
        order.setStatus(RequestStatus.REJECTED);
        return order;
    }

    @Override
    public ExtraOrder getOrderById(Long orderId) {
        if (orderId == null || orderId <= 0) throw new ValidationException("Invalid order ID.");
        return extraOrderDAO.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Extra order not found with ID: " + orderId));
    }

    @Override
    public List<ExtraOrder> getPendingOrders() {
        return extraOrderDAO.findPendingOrders();
    }

    @Override
    public List<ExtraOrder> getCustomerOrders(Long customerId) {
        if (customerId == null || customerId <= 0) throw new ValidationException("Invalid customer ID.");
        return extraOrderDAO.findByCustomerId(customerId);
    }

    @Override
    public List<ExtraOrder> getOrdersByDeliveryDate(Date deliveryDate) {
        if (deliveryDate == null) throw new ValidationException("Delivery date is required.");
        return extraOrderDAO.findByDeliveryDate(deliveryDate);
    }
}
