package com.example.OMS.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String productName, Integer available, Integer requested) {
        super(String.format("Insufficient stock for '%s': available %d, requested %d",
                productName, available, requested));
    }
}