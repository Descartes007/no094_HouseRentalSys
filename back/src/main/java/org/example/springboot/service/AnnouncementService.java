package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.Announcement;
import org.example.springboot.entity.User;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.AnnouncementMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 公告服务类
 */
@Service
public class AnnouncementService {

    @Resource
    private AnnouncementMapper announcementMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 分页查询公告列表
     *
     * @param title 标题关键词
     * @param status 状态
     * @param currentPage 当前页
     * @param size 每页大小
     * @return 分页结果
     */
    public Page<Announcement> getAnnouncementsByPage(String title, Integer status, Integer currentPage, Integer size) {
        Page<Announcement> page = new Page<>(currentPage, size);
        LambdaQueryWrapper<Announcement> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(title)) {
            queryWrapper.like(Announcement::getTitle, title);
        }
        if (status != null) {
            queryWrapper.eq(Announcement::getStatus, status);
        }
        
        // 按创建时间倒序排序
        queryWrapper.orderByDesc(Announcement::getCreateTime);
        
        // 执行查询
        Page<Announcement> resultPage = announcementMapper.selectPage(page, queryWrapper);
        
        // 填充管理员信息
        resultPage.getRecords().forEach(this::fillAdminInfo);
        
        return resultPage;
    }

    /**
     * 根据ID获取公告
     *
     * @param id 公告ID
     * @return 公告对象
     */
    public Announcement getById(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new ServiceException("公告不存在");
        }
        
        // 填充管理员信息
        fillAdminInfo(announcement);
        
        return announcement;
    }

    /**
     * 发布公告
     *
     * @param announcement 公告对象
     */
    @Transactional
    public void createAnnouncement(Announcement announcement) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查用户是否为管理员
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("只有管理员可以发布公告");
        }
        
        // 设置管理员ID
        announcement.setAdminId(currentUser.getId());
        
        // 设置默认状态为发布
        if (announcement.getStatus() == null) {
            announcement.setStatus(1);
        }
        
        // 设置创建时间和更新时间
        LocalDateTime now = LocalDateTime.now();
        announcement.setCreateTime(now);
        announcement.setUpdateTime(now);
        
        // 插入数据库
        announcementMapper.insert(announcement);
    }

    /**
     * 更新公告
     *
     * @param announcement 公告对象
     */
    @Transactional
    public void updateAnnouncement(Announcement announcement) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查用户是否为管理员
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("只有管理员可以更新公告");
        }
        
        // 检查公告是否存在
        Announcement existingAnnouncement = announcementMapper.selectById(announcement.getId());
        if (existingAnnouncement == null) {
            throw new ServiceException("公告不存在");
        }
        
        // 设置更新时间
        announcement.setUpdateTime(LocalDateTime.now());
        
        // 更新数据库
        announcementMapper.updateById(announcement);
    }

    /**
     * 删除公告
     *
     * @param id 公告ID
     */
    @Transactional
    public void deleteAnnouncement(Long id) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查用户是否为管理员
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("只有管理员可以删除公告");
        }
        
        // 检查公告是否存在
        Announcement existingAnnouncement = announcementMapper.selectById(id);
        if (existingAnnouncement == null) {
            throw new ServiceException("公告不存在");
        }
        
        // 删除公告
        announcementMapper.deleteById(id);
    }

    /**
     * 修改公告状态
     *
     * @param id 公告ID
     * @param status 状态
     */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        // 获取当前用户
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("获取当前用户信息失败");
        }
        
        // 检查用户是否为管理员
        if (!"ADMIN".equals(currentUser.getRoleCode())) {
            throw new ServiceException("只有管理员可以修改公告状态");
        }
        
        // 检查公告是否存在
        Announcement existingAnnouncement = announcementMapper.selectById(id);
        if (existingAnnouncement == null) {
            throw new ServiceException("公告不存在");
        }
        
        // 检查状态是否有效
        if (status != 0 && status != 1) {
            throw new ServiceException("无效的状态值");
        }
        
        // 更新状态
        Announcement announcement = new Announcement();
        announcement.setId(id);
        announcement.setStatus(status);
        announcement.setUpdateTime(LocalDateTime.now());
        
        announcementMapper.updateById(announcement);
    }
    
    /**
     * 填充管理员信息
     *
     * @param announcement 公告对象
     */
    private void fillAdminInfo(Announcement announcement) {
        if (announcement != null && announcement.getAdminId() != null) {
            User admin = userMapper.selectById(announcement.getAdminId());
            if (admin != null) {
                // 清除敏感信息
                admin.setPassword(null);
                announcement.setAdmin(admin);
            }
        }
    }
} 