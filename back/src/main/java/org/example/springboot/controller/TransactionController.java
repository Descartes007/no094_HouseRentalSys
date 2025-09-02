package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Transaction;
import org.example.springboot.service.TransactionService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@Tag(name = "交易记录管理接口")
@RestController
@RequestMapping("/transactions")
public class TransactionController {
    @Resource
    private TransactionService transactionService;
    
    @Operation(summary = "获取当前用户的交易记录")
    @GetMapping
    public Result<?> getUserTransactions(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(transactionService.getUserTransactions(type, currentPage, size));
    }
    
    @Operation(summary = "管理员获取所有交易记录")
    @GetMapping("/admin")
    public Result<?> getAllTransactions(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(transactionService.getAllTransactions(username, type, currentPage, size));
    }
} 