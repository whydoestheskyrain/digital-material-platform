package com.trade.virtual_trade.controller;

import com.trade.virtual_trade.common.Result;
import com.trade.virtual_trade.entity.PetSkin;
import com.trade.virtual_trade.entity.UserPet;
import com.trade.virtual_trade.service.PetAiService;
import com.trade.virtual_trade.service.PetSkinService;
import com.trade.virtual_trade.service.UserPetService;
import com.trade.virtual_trade.utils.JwtUtil;
import com.trade.virtual_trade.security.CurrentUser;
import com.trade.virtual_trade.security.RequireAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pet")
public class PetController {

    @Autowired
    private PetAiService petAiService;

    @Autowired
    private PetSkinService petSkinService;

    @Autowired
    private UserPetService userPetService;

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, String> params,
                                            @RequestHeader(value = "Authorization", required = false) String token) {
        String msg = params.get("msg");
        if (msg == null || msg.trim().isEmpty()) {
            return Result.error("请输入问题");
        }
        // sessionId 由前端维护，首次为空时后端生成并回传，保证多轮记忆连贯
        String sessionId = params.get("sessionId");
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = java.util.UUID.randomUUID().toString().replace("-", "");
        }
        Integer userId = parseUserId(token);
        String reply = petAiService.chat(msg.trim(), sessionId, userId);

        Map<String, Object> data = new HashMap<>();
        data.put("reply", reply);
        data.put("sessionId", sessionId);
        return Result.success(data);
    }

    /**
     * SSE 流式对话：逐字返回 AI 回复，实现打字机效果。
     * 前端用 fetch + ReadableStream 读取 text/event-stream。
     */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream")
    public SseEmitter streamChat(@RequestParam String msg,
                                 @RequestParam(required = false) String sessionId,
                                 @RequestHeader(value = "Authorization", required = false) String token) {
        SseEmitter emitter = new SseEmitter(30000L); // 30 秒超时
        Integer userId = parseUserId(token);
        petAiService.streamChat(msg, sessionId, userId, emitter);
        return emitter;
    }

    /**
     * 从 Authorization 头解析用户ID，解析失败返回 null（降级为无业务上下文）
     */
    private Integer parseUserId(String token) {
        if (token == null || token.length() <= 7) {
            return null;
        }
        try {
            return JwtUtil.getUserId(token.substring(7));
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/user/get")
    public Result<Map<String, Object>> getUserPet(@RequestParam Integer userId, HttpServletRequest request) {
        if (!isSelfOrAdmin(userId, request)) {
            return Result.error("无权查看其他用户的桌宠配置");
        }
        Map<String, Object> result = new HashMap<>();
        UserPet userPet = userPetService.getByUserId(userId);
        
        if (userPet != null) {
            result.put("posX", userPet.getPosX());
            result.put("posY", userPet.getPosY());
            result.put("skinId", userPet.getSkinId());
        } else {
            result.put("posX", 1300);
            result.put("posY", 650);
            result.put("skinId", null);
        }
        
        PetSkin defaultSkin = petSkinService.getDefault();
        if (defaultSkin != null) {
            result.put("skin", defaultSkin);
        }
        
        return Result.success(result);
    }

    @PostMapping("/user/save")
    public Result saveUserPet(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        // userId 来自已验证 Token，忽略客户端提交的 userId。
        Integer userId = CurrentUser.getUserId(request);
        Integer skinId = ((Number) params.get("skinId")).intValue();
        Integer posX = ((Number) params.get("posX")).intValue();
        Integer posY = ((Number) params.get("posY")).intValue();
        
        userPetService.saveOrUpdate(userId, skinId, posX, posY);
        return Result.success();
    }

    @GetMapping("/skin/all")
    public Result<List<PetSkin>> getAllSkins() {
        return Result.success(petSkinService.getAvailable());
    }

    @GetMapping("/skin/list")
    @RequireAdmin
    public Result<List<PetSkin>> getSkinList() {
        return Result.success(petSkinService.getAll());
    }

    @GetMapping("/skin/detail/{id}")
    public Result<PetSkin> getSkinDetail(@PathVariable Integer id) {
        PetSkin skin = petSkinService.getById(id);
        if (skin == null) {
            return Result.error("皮肤不存在");
        }
        return Result.success(skin);
    }

    @PostMapping("/skin/add")
    @RequireAdmin
    public Result addSkin(@RequestBody PetSkin petSkin) {
        petSkinService.add(petSkin);
        return Result.success();
    }

    @PutMapping("/skin/update")
    @RequireAdmin
    public Result updateSkin(@RequestBody PetSkin petSkin) {
        petSkinService.update(petSkin);
        return Result.success();
    }

    @DeleteMapping("/skin/delete/{id}")
    @RequireAdmin
    public Result deleteSkin(@PathVariable Integer id) {
        petSkinService.delete(id);
        return Result.success();
    }

    @PostMapping("/skin/setDefault/{id}")
    @RequireAdmin
    public Result setDefaultSkin(@PathVariable Integer id) {
        petSkinService.setDefault(id);
        return Result.success();
    }

    private boolean isSelfOrAdmin(Integer userId, HttpServletRequest request) {
        return userId != null && (userId.equals(CurrentUser.getUserId(request)) || CurrentUser.isAdmin(request));
    }
}
