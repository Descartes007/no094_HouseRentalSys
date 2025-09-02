package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.entity.*;
import org.example.springboot.enumClass.OrderStatus;
import org.example.springboot.enumClass.TransactionType;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.HouseMapper;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.TransactionMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@Service
public class OrderService {
    @Resource
    private OrderMapper orderMapper;
    
    @Resource
    private HouseMapper houseMapper;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private TransactionMapper transactionMapper;
    
    @Resource
    private LeaseRecordService leaseRecordService;
    
    /**
     * 创建订单
     */
    @Transactional
    public Order createOrder(Order order) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询房屋信息
        House house = houseMapper.selectById(order.getHouseId());
        if (house == null) {
            throw new ServiceException("房屋不存在");
        }
        
        // 检查房屋状态是否可租
        if (house.getStatus() != 1) {
            throw new ServiceException("该房屋不可租用");
        }
        
        // 设置订单基本信息
        order.setTenantId(user.getId());
        order.setLandlordId(house.getLandlordId());
        order.setOrderNo(generateOrderNo());
        order.setStatus(OrderStatus.WAITING_PAYMENT.getValue());
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        
        // 保存订单
        orderMapper.insert(order);
        
        return order;
    }
    
    /**
     * 支付订单
     */
    @Transactional
    public Order payOrder(Long orderId, String paymentMethod) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证操作权限
        if (!order.getTenantId().equals(user.getId())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态
        if (!order.getStatus().equals(OrderStatus.WAITING_PAYMENT.getValue())) {
            throw new ServiceException("订单状态不正确，无法支付");
        }
        
        // 更新订单状态为已支付待确认
        order.setStatus(OrderStatus.PAID_WAITING_CONFIRM.getValue());
        order.setPaymentTime(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 创建租客支出交易记录
        Transaction tenantExpense = new Transaction();
        tenantExpense.setOrderId(orderId);
        tenantExpense.setUserId(user.getId());
        tenantExpense.setType(TransactionType.EXPENSE.getValue());
        tenantExpense.setAmount(order.getAmount());
        tenantExpense.setDescription("租金支付-订单号:" + order.getOrderNo());
        tenantExpense.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tenantExpense);
        
        // 创建房东收入交易记录
        Transaction landlordIncome = new Transaction();
        landlordIncome.setOrderId(orderId);
        landlordIncome.setUserId(order.getLandlordId());
        landlordIncome.setType(TransactionType.INCOME.getValue());
        landlordIncome.setAmount(order.getAmount());
        landlordIncome.setDescription("租金收入-订单号:" + order.getOrderNo());
        landlordIncome.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(landlordIncome);
        
        return order;
    }
    
    /**
     * 确认订单
     */
    @Transactional
    public Order confirmOrder(Long orderId) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证操作权限，只有房东可以确认订单
        if (!order.getLandlordId().equals(user.getId()) && !"ADMIN".equals(user.getRoleCode())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态，只有已支付待确认的订单可以确认
        if (!order.getStatus().equals(OrderStatus.PAID_WAITING_CONFIRM.getValue())) {
            throw new ServiceException("订单状态不正确，无法确认");
        }
        
        // 获取房屋信息
        House house = houseMapper.selectById(order.getHouseId());
        if (house == null) {
            throw new ServiceException("房屋信息不存在");
        }
        
        // 更新房屋状态为已出租
        house.setStatus(2); // 2表示已出租
        houseMapper.updateById(house);
        
        // 更新订单状态为已确认
        order.setStatus(OrderStatus.CONFIRMED.getValue());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 创建租赁记录并生成合同
        LeaseRecord leaseRecord = leaseRecordService.createLeaseRecord(order);
        log.info("创建租赁记录成功，ID：{}", leaseRecord.getId());
        
        return order;
    }
    
    /**
     * 取消订单
     */
    @Transactional
    public Order cancelOrder(Long orderId) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证操作权限
        if (!order.getTenantId().equals(user.getId())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态，只有待支付的订单可以取消
        if (!order.getStatus().equals(OrderStatus.WAITING_PAYMENT.getValue())) {
            throw new ServiceException("订单状态不正确，无法取消");
        }
        
        // 更新订单状态为已取消
        order.setStatus(OrderStatus.CANCELED.getValue());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        return order;
    }
    
    /**
     * 申请退款
     */
    @Transactional
    public Order refundOrder(Long orderId) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        
        // 验证操作权限，只有租客可以申请退款
        if (!order.getTenantId().equals(user.getId())) {
            throw new ServiceException("无权操作此订单");
        }
        
        // 检查订单状态，只有已支付待确认的订单可以退款
        if (!order.getStatus().equals(OrderStatus.PAID_WAITING_CONFIRM.getValue())) {
            throw new ServiceException("订单状态不正确，无法申请退款");
        }
        
        // 更新订单状态为已退款
        order.setStatus(OrderStatus.REFUNDED.getValue());
        order.setUpdateTime(LocalDateTime.now());
        orderMapper.updateById(order);
        
        // 创建租客收入交易记录（退款）
        Transaction tenantIncome = new Transaction();
        tenantIncome.setOrderId(orderId);
        tenantIncome.setUserId(user.getId());
        tenantIncome.setType(TransactionType.INCOME.getValue());
        tenantIncome.setAmount(order.getAmount());
        tenantIncome.setDescription("退款收入-订单号:" + order.getOrderNo());
        tenantIncome.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(tenantIncome);
        
        // 创建房东支出交易记录（退款）
        Transaction landlordExpense = new Transaction();
        landlordExpense.setOrderId(orderId);
        landlordExpense.setUserId(order.getLandlordId());
        landlordExpense.setType(TransactionType.EXPENSE.getValue());
        landlordExpense.setAmount(order.getAmount());
        landlordExpense.setDescription("退款支出-订单号:" + order.getOrderNo());
        landlordExpense.setCreateTime(LocalDateTime.now());
        transactionMapper.insert(landlordExpense);
        
        return order;
    }
    
    /**
     * 获取订单详情
     */
    public Order getOrderDetail(Long orderId) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        

        
        // 获取关联信息
        order.setHouse(houseMapper.selectById(order.getHouseId()));
        order.setTenant(userMapper.selectById(order.getTenantId()));
        order.setLandlord(userMapper.selectById(order.getLandlordId()));
        
        return order;
    }
    
    /**
     * 租客获取自己的订单列表
     */
    public Page<Order> getTenantOrders(String orderNo, Integer status, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getTenantId, user.getId());
        
        // 添加查询条件
        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        // 分页查询
        Page<Order> page = orderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Order order : page.getRecords()) {
            order.setHouse(houseMapper.selectById(order.getHouseId()));
            order.setLandlord(userMapper.selectById(order.getLandlordId()));
        }
        
        return page;
    }
    
    /**
     * 房东获取自己的订单列表
     */
    public Page<Order> getLandlordOrders(String orderNo, Integer status, Integer currentPage, Integer size) {
        // 获取当前登录用户
        User user = JwtTokenUtils.getCurrentUser();
        if (user == null) {
            throw new ServiceException("请先登录");
        }
        
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Order::getLandlordId, user.getId());
        
        // 添加查询条件
        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        // 分页查询
        Page<Order> page = orderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Order order : page.getRecords()) {
            order.setHouse(houseMapper.selectById(order.getHouseId()));
            order.setTenant(userMapper.selectById(order.getTenantId()));
        }
        
        return page;
    }
    
    /**
     * 管理员获取所有订单列表
     */
    public Page<Order> getAllOrders(String orderNo, Integer status, String tenantUsername, String landlordUsername, Integer currentPage, Integer size) {
        // 如果有用户名查询条件，先查询用户ID
        Long tenantId = null;
        Long landlordId = null;
        

        
        LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(tenantUsername)) {
            LambdaQueryWrapper<User> tenantQuery = new LambdaQueryWrapper<>();
            tenantQuery.like(User::getUsername, tenantUsername);
            List<User> tenants = userMapper.selectList(tenantQuery);
            List<Long> tenantIds = tenants.stream().map(User::getId).collect(Collectors.toList());
            if(!tenantIds.isEmpty()){
                queryWrapper.in(Order::getTenantId, tenantIds);
            }else{
                return new Page<>(currentPage, size);
            }

        }

        if (StringUtils.isNotBlank(landlordUsername)) {
            LambdaQueryWrapper<User> landlordQuery = new LambdaQueryWrapper<>();
            landlordQuery.like(User::getUsername, landlordUsername);
            List<User> landlords = userMapper.selectList(landlordQuery);
            List<Long> landlordIds = landlords.stream().map(User::getId).collect(Collectors.toList());
            if(!landlordIds.isEmpty()){
                queryWrapper.in(Order::getLandlordId, landlordIds);
            }else {
                return new Page<>(currentPage, size);
            }


        }
        
        // 添加查询条件
        if (StringUtils.isNotBlank(orderNo)) {
            queryWrapper.like(Order::getOrderNo, orderNo);
        }
        if (status != null) {
            queryWrapper.eq(Order::getStatus, status);
        }
        if (tenantId != null) {
            queryWrapper.eq(Order::getTenantId, tenantId);
        }
        if (landlordId != null) {
            queryWrapper.eq(Order::getLandlordId, landlordId);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Order::getCreateTime);
        
        // 分页查询
        Page<Order> page = orderMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
        
        // 填充关联信息
        for (Order order : page.getRecords()) {
            order.setHouse(houseMapper.selectById(order.getHouseId()));
            order.setTenant(userMapper.selectById(order.getTenantId()));
            order.setLandlord(userMapper.selectById(order.getLandlordId()));
        }
        
        return page;
    }
    
    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4);
    }
} 