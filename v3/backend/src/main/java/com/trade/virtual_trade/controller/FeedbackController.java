package com.trade.virtual_trade.controller;

import com.trade.virtual_trade.common.Result;
import com.trade.virtual_trade.entity.Feedback;
import com.trade.virtual_trade.service.FeedbackService;
import com.trade.virtual_trade.service.NotificationService;
import com.trade.virtual_trade.security.CurrentUser;
import com.trade.virtual_trade.security.RequireAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/submit")
    public Result submit(@RequestBody Feedback feedback, HttpServletRequest request) {
        // 反馈归属由当前 Token 决定，不能由请求体伪造。
        feedback.setUserId(CurrentUser.getUserId(request));
        feedbackService.submit(feedback);
        return Result.success();
    }

    @GetMapping("/list/{userId}")
    public Result list(@PathVariable Integer userId, HttpServletRequest request) {
        if (!isSelfOrAdmin(userId, request)) {
            return Result.error("无权查看其他用户的反馈");
        }
        return Result.success(feedbackService.getByUserId(userId));
    }

    @GetMapping("/admin/list")
    @RequireAdmin
    public Result adminList() {
        return Result.success(feedbackService.getAll());
    }

    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Integer id, HttpServletRequest request) {
        Feedback feedback = feedbackService.getById(id);
        if (feedback == null) {
            return Result.error("反馈不存在");
        }
        if (!isSelfOrAdmin(feedback.getUserId(), request)) {
            return Result.error("无权查看此反馈");
        }
        return Result.success(feedback);
    }

    @PostMapping("/reply/{id}")
    @RequireAdmin
    public Result reply(@PathVariable Integer id, @RequestParam String reply) {
        feedbackService.reply(id, reply);
        Feedback feedback = feedbackService.getById(id);
        if (feedback != null) {
            notificationService.sendFeedbackNotification(
                feedback.getUserId(),
                "反馈已回复",
                "您的反馈「" + feedback.getTitle() + "」已收到回复：" + reply
            );
        }
        return Result.success();
    }

    @PutMapping("/close/{id}")
    @RequireAdmin
    public Result close(@PathVariable Integer id) {
        feedbackService.close(id);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @RequireAdmin
    public Result delete(@PathVariable Integer id) {
        feedbackService.delete(id);
        return Result.success();
    }

    private boolean isSelfOrAdmin(Integer userId, HttpServletRequest request) {
        return userId != null && (userId.equals(CurrentUser.getUserId(request)) || CurrentUser.isAdmin(request));
    }
}
