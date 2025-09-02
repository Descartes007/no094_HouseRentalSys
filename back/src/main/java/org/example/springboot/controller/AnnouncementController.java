package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Announcement;
import org.example.springboot.service.AnnouncementService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 公告控制器
 */
@Tag(name = "公告管理接口")
@RestController
public class AnnouncementController {

    @Resource
    private AnnouncementService announcementService;

    /**
     * 获取公告列表
     */
    @Operation(summary = "获取公告列表")
    @GetMapping("/announcements")
    public Result<?> getAnnouncements(
            @RequestParam(defaultValue = "") String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(announcementService.getAnnouncementsByPage(title, status, currentPage, size));
    }

    /**
     * 获取公告详情
     */
    @Operation(summary = "获取公告详情")
    @GetMapping("/announcements/{id}")
    public Result<?> getAnnouncementById(@PathVariable Long id) {
        return Result.success(announcementService.getById(id));
    }

    /**
     * 发布公告（仅管理员）
     */
    @Operation(summary = "发布公告")
    @PostMapping("/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> createAnnouncement(@RequestBody Announcement announcement) {
        announcementService.createAnnouncement(announcement);
        return Result.success("发布成功");
    }

    /**
     * 更新公告（仅管理员）
     */
    @Operation(summary = "更新公告")
    @PutMapping("/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> updateAnnouncement(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        announcementService.updateAnnouncement(announcement);
        return Result.success("更新成功");
    }

    /**
     * 删除公告（仅管理员）
     */
    @Operation(summary = "删除公告")
    @DeleteMapping("/admin/announcements/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return Result.success("删除成功");
    }

    /**
     * 修改公告状态（仅管理员）
     */
    @Operation(summary = "修改公告状态")
    @PutMapping("/admin/announcements/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<?> updateAnnouncementStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        announcementService.updateStatus(id, status);
        return Result.success("状态修改成功");
    }
} 