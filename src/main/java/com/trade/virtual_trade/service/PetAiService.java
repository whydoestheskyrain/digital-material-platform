package com.trade.virtual_trade.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface PetAiService {

    /**
     * 普通对话（兼容旧接口，无 sessionId、无业务上下文）
     */
    String getAiReply(String userMsg);

    /**
     * 带会话记忆和业务上下文的对话
     *
     * @param userMsg   用户消息
     * @param sessionId 会话ID（首次传空，后端会生成并返回）；用于多轮记忆
     * @param userId    用户ID（可为空）；用于查询余额、订单等业务数据
     * @return AI 回复文本
     */
    String chat(String userMsg, String sessionId, Integer userId);

    /**
     * 流式对话（SSE），逐字返回
     *
     * @param userMsg   用户消息
     * @param sessionId 会话ID
     * @param userId    用户ID
     * @param emitter   SSE 发射器
     */
    void streamChat(String userMsg, String sessionId, Integer userId, SseEmitter emitter);
}
