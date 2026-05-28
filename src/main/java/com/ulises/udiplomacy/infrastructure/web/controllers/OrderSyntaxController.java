package com.ulises.udiplomacy.infrastructure.web.controllers;

import com.ulises.udiplomacy.application.port.input.GetOrderSyntaxUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderSyntaxController {
    private final GetOrderSyntaxUseCase getOrderSyntaxUseCase;

    public OrderSyntaxController(GetOrderSyntaxUseCase getOrderSyntaxUseCase) {
        this.getOrderSyntaxUseCase = getOrderSyntaxUseCase;
    }

    @GetMapping("/syntax")
    public ResponseEntity<Map<String, Object>> getSyntax() {
        return ResponseEntity.ok(getOrderSyntaxUseCase.execute());
    }
}
