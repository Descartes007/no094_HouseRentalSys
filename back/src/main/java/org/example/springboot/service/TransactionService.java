package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Transaction;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.TransactionMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    @Resource
    private TransactionMapper transactionMapper;
    
    @Resource
    private OrderMapper orderMapper;
    
    @Resource
    private UserMapper userMapper;
    
    /**
     * 获取当前用户的交易记录
     */
    public Page<Transaction> getUserTransactions(Integer type, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Transaction::getUserId, user.getId());
        
        // 添加类型查询条件
        if (type != null) {
            queryWrapper.eq(Transaction::getType, type);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Transaction::getCreateTime);
        
        // 分页查询
        Page<Transaction> page = transactionMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Transaction transaction : page.getRecords()) {
            Order order = orderMapper.selectById(transaction.getOrderId());
            transaction.setOrder(order);
        }
        
        return page;
    }
    
    /**
     * 管理员获取所有交易记录
     */
    public Page<Transaction> getAllTransactions(String username, Integer type, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        LambdaQueryWrapper<Transaction> queryWrapper = new LambdaQueryWrapper<>();
        
        // 如果有用户名查询条件，先查询用户ID
        if (org.apache.commons.lang3.StringUtils.isNotBlank(username)) {
            LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
            userQuery.like(User::getUsername, username);
            List<User> users = userMapper.selectList(userQuery);
            List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
            if (!userIds.isEmpty()) {
                queryWrapper.in(Transaction::getUserId, userIds);
            } else {
                // 如果没有找到匹配的用户，返回空结果
                return new Page<>(currentPage, size);
            }
        }
        
        // 添加类型查询条件
        if (type != null) {
            queryWrapper.eq(Transaction::getType, type);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Transaction::getCreateTime);
        
        // 分页查询
        Page<Transaction> page = transactionMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Transaction transaction : page.getRecords()) {
            transaction.setOrder(orderMapper.selectById(transaction.getOrderId()));
            transaction.setUser(userMapper.selectById(transaction.getUserId()));
        }
        
        return page;
    }
    
    /**
     * 创建交易记录
     */
    public Transaction createTransaction(Transaction transaction) {
        transaction.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(transaction);
        return transaction;
    }
} 