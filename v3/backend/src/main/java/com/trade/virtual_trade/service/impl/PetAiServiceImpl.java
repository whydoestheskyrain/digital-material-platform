package com.trade.virtual_trade.service.impl;

import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.trade.virtual_trade.service.PetAiService;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

@Service
public class PetAiServiceImpl implements PetAiService {

    private static final Logger log = LoggerFactory.getLogger(PetAiServiceImpl.class);

    /** 单用户每分钟最大请求数（防刷 token） */
    private static final int RATE_LIMIT_PER_MINUTE = 15;

    /** 敏感词列表（命中则拒答） */
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "赌博", "博彩", "彩票", "色情", "裸聊", "毒品", "枪支", "弹药", "炸药",
            "恐怖主义", "分裂国家", "颠覆政权", "反动", "邪教", "传销", "诈骗",
            "洗钱", "套现", "刷单"
    );

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.app-id:}")
    private String appId;

    @Autowired
    private PetAiToolService toolService;

    /** 限流记录：userId -> 时间戳队列 */
    private final ConcurrentHashMap<Integer, ConcurrentLinkedDeque<Long>> rateLimitMap = new ConcurrentHashMap<>();

    private final String SYSTEM_PROMPT = "你是「虚拟交易平台」的AI客服，名叫像素少女，以桌宠形象陪伴用户。\n" +
            "【人设与语气】说话温柔、亲切、简短，可用「哦、啦、呢、~」等语气词，每次回复控制在2-4句话，不长篇大论，不堆砌套话。\n" +
            "【服务范围】只解答本游戏账号交易平台的问题：注册登录、资料修改、商品发布与审核、浏览购买、下单支付、充值、发货收货、资金结算、订单取消、消息通知、桌宠皮肤。\n" +
            "【平台真实规则，必须严格遵守，不得编造】" +
            "1.交易流程：买家下单→付款（余额支付或支付宝沙箱充值后余额支付）→资金由平台担保冻结托管→卖家在订单中填写游戏账号信息完成发货（账号信息发货后买家才可见）→买家确认收货→冻结资金才打给卖家。" +
            "2.下单后15分钟内未支付，订单会被系统自动取消；待支付状态买家也可手动取消，余额支付的款项原路退回余额。" +
            "3.每个买家最多同时拥有3笔进行中的订单（待支付/已支付/已发货），超过需先完成或取消已有订单。" +
            "4.商品发布后需管理员审核通过才会上架。" +
            "5.平台没有承诺发货时限、没有投诉或申请平台介入按钮、没有余额提现功能。被问到卖家多久发货时，如实告知暂无强制时限，建议通过订单消息联系卖家，或向管理员反馈；被问提现时告知当前为演示平台、余额仅用于站内交易。" +
            "6.支付走支付宝沙箱环境，不会产生真实扣款。\n" +
            "【禁止事项】不得虚构平台不存在的功能、按钮、时限和数字；不确定的问题不要瞎编，引导用户联系管理员。\n" +
            "【无关问题】用户询问动漫、游戏攻略、生活闲聊等与平台无关的内容，统一回复：我只解答本平台交易相关问题哦~\n" +
            "【回答示范】\n" +
            "用户：怎么发布商品？\n" +
            "AI：点击首页的「发布」按钮，填写商品信息提交即可~管理员审核通过后就会上架哦。\n" +
            "用户：付款后钱去哪了？\n" +
            "AI：付款后资金由平台担保托管，您确认收货后才会打给卖家哦~这样能保障双方交易安全呢。\n" +
            "用户：我想提现\n" +
            "AI：抱歉呀，当前是演示平台，余额仅用于站内交易，暂不支持提现哦~";

    private final List<String> MOCK_REPLIES = new ArrayList<>();

    public PetAiServiceImpl() {
        MOCK_REPLIES.add("您好呀~ 请问有什么可以帮助您的呢？");
        MOCK_REPLIES.add("关于注册登录的问题，您可以点击页面右上角的登录按钮哦~");
        MOCK_REPLIES.add("发布商品很简单的，点击首页的发布按钮就可以啦，提交后等待管理员审核哦~");
        MOCK_REPLIES.add("订单相关问题，请在个人中心查看我的订单哦~");
        MOCK_REPLIES.add("充值走支付宝沙箱环境，不会产生真实扣款，请放心测试~");
        MOCK_REPLIES.add("桌宠皮肤可以在个人中心切换哦，有很多可爱形象~");
        MOCK_REPLIES.add("我只解答本平台交易相关问题哦~");
        MOCK_REPLIES.add("付款后资金由平台担保托管，您确认收货后才会打给卖家哦~");
        MOCK_REPLIES.add("下单后15分钟内未支付，订单会被自动取消呢，待支付状态也可以手动取消~");
        MOCK_REPLIES.add("卖家发货后您才能看到游戏账号信息，确认收货后款项才会结算给卖家哦~");
    }

    @Override
    public String getAiReply(String userMsg) {
        return chat(userMsg, null, null);
    }

    @Override
    public String chat(String userMsg, String sessionId, Integer userId) {
        // 1. 输入校验
        if (userMsg == null || userMsg.trim().isEmpty()) {
            return "请问有什么可以帮您的呢~";
        }
        userMsg = userMsg.trim();

        // 2. 敏感词过滤
        if (containsSensitiveWord(userMsg)) {
            log.info("[AI] 敏感词拦截 user={} msg={}", userId, userMsg);
            return "这个问题我无法回答哦~我只解答本平台交易相关问题呢。";
        }

        // 3. 限流
        if (userId != null && !checkRateLimit(userId)) {
            log.warn("[AI] 触发限流 user={}", userId);
            return "您提问太快啦，稍微休息一下再问哦~";
        }

        // 4. 生成 sessionId（多轮记忆）
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }

        // 5. 未配置 API → Mock
        if (!isValidApiKeyAndAppId()) {
            String reply = getMockReply(userMsg);
            log.info("[AI][MOCK] user={} msg={} reply={}", userId, userMsg, reply);
            return reply;
        }

        // 6. 真实 AI：注入业务上下文 + 多轮记忆
        String context = buildBusinessContext(userMsg, userId);
        String reply = callDashScope(userMsg, sessionId, context);
        log.info("[AI] user={} session={} msg={} reply={}", userId, sessionId, userMsg, reply);
        return reply;
    }

    @Override
    public void streamChat(String userMsg, String sessionId, Integer userId, SseEmitter emitter) {
        // 输入校验
        if (userMsg == null || userMsg.trim().isEmpty()) {
            sendEventAndComplete(emitter, "请问有什么可以帮您的呢~");
            return;
        }
        userMsg = userMsg.trim();
        final String finalMsg = userMsg;

        if (containsSensitiveWord(userMsg)) {
            log.info("[AI] 敏感词拦截(流式) user={} msg={}", userId, userMsg);
            sendEventAndComplete(emitter, "这个问题我无法回答哦~我只解答本平台交易相关问题呢。");
            return;
        }

        if (userId != null && !checkRateLimit(userId)) {
            log.warn("[AI] 触发限流(流式) user={}", userId);
            sendEventAndComplete(emitter, "您提问太快啦，稍微休息一下再问哦~");
            return;
        }

        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }

        // 未配置 API → 直接返回 Mock 完整文本（不流式）
        if (!isValidApiKeyAndAppId()) {
            String reply = getMockReply(userMsg);
            log.info("[AI][MOCK][流式] user={} msg={}", userId, userMsg);
            sendEventAndComplete(emitter, reply);
            return;
        }

        String context = buildBusinessContext(userMsg, userId);
        log.info("[AI][流式] user={} session={} msg={}", userId, sessionId, userMsg);

        try {
            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(apiKey)
                    .appId(appId)
                    .sessionId(sessionId)
                    .prompt(SYSTEM_PROMPT + context + "\n用户提问：" + userMsg)
                    .build();

            Application app = new Application();
            Flowable<ApplicationResult> flowable = app.streamCall(param);

            final StringBuilder fullText = new StringBuilder();
            flowable.subscribe(
                    result -> {
                        if (result != null && result.getOutput() != null
                                && result.getOutput().getText() != null) {
                            String chunk = result.getOutput().getText();
                            fullText.append(chunk);
                            try {
                                emitter.send(SseEmitter.event().data(chunk));
                            } catch (IOException e) {
                                log.warn("[AI][流式] 发送分片失败: {}", e.getMessage());
                            }
                        }
                    },
                    error -> {
                        log.error("[AI][流式] DashScope 调用失败: {}", error.getMessage());
                        // 降级：如果还没发出任何内容，返回 Mock
                        if (fullText.length() == 0) {
                            try {
                                emitter.send(SseEmitter.event().data(getMockReply(finalMsg)));
                            } catch (IOException ignored) {
                            }
                        }
                        emitter.complete();
                    },
                    () -> {
                        log.info("[AI][流式] 完成 user={} reply={}", userId, fullText.toString());
                        emitter.complete();
                    }
            );
        } catch (Exception e) {
            log.error("[AI][流式] 异常: {}", e.getMessage());
            sendEventAndComplete(emitter, getMockReply(finalMsg));
        }
    }

    // ===================== 内部方法 =====================

    private boolean isValidApiKeyAndAppId() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.startsWith("sk-your-api-key")
                && appId != null && !appId.isEmpty() && !appId.equals("your-app-id-here");
    }

    /**
     * 调用 DashScope 同步接口，带 sessionId 多轮记忆
     */
    private String callDashScope(String userMsg, String sessionId, String context) {
        try {
            ApplicationParam param = ApplicationParam.builder()
                    .apiKey(apiKey)
                    .appId(appId)
                    .sessionId(sessionId)
                    .prompt(SYSTEM_PROMPT + context + "\n用户提问：" + userMsg)
                    .build();

            Application app = new Application();
            ApplicationResult result = app.call(param);

            if (result != null && result.getOutput() != null
                    && result.getOutput().getText() != null
                    && !result.getOutput().getText().isEmpty()) {
                return result.getOutput().getText();
            }
        } catch (Exception e) {
            log.error("DashScope API call failed: {}", e.getMessage());
        }
        return getMockReply(userMsg);
    }

    /**
     * 意图识别 + 业务上下文注入。
     * 根据用户消息匹配意图，查询真实数据并拼成上下文文本，让 AI 有据可答。
     */
    private String buildBusinessContext(String userMsg, Integer userId) {
        if (userId == null) {
            return "";
        }
        String lower = userMsg.toLowerCase();
        StringBuilder ctx = new StringBuilder("\n【用户业务数据（仅供回答参考，如实告知，不要编造）】\n");
        boolean hasContext = false;

        // 余额相关
        if (lower.contains("余额") || lower.contains("多少钱") || lower.contains("账户")) {
            ctx.append("- ").append(toolService.getBalanceContext(userId)).append("\n");
            hasContext = true;
        }

        // 买家订单统计
        if (lower.contains("买到") || lower.contains("买入")
                || (lower.contains("订单") && (lower.contains("买") || lower.contains("付") || lower.contains("待")))) {
            ctx.append("- ").append(toolService.getBuyerOrderContext(userId)).append("\n");
            hasContext = true;
        }

        // 卖家订单统计
        if (lower.contains("卖出") || lower.contains("我发布的") || lower.contains("待发货")
                || (lower.contains("订单") && lower.contains("卖"))) {
            ctx.append("- ").append(toolService.getSellerOrderContext(userId)).append("\n");
            hasContext = true;
        }

        // 最新订单状态（"我的订单到哪了"类问题）
        if (lower.contains("到哪") || lower.contains("进度") || lower.contains("状态")
                || (lower.contains("订单") && !lower.contains("统计") && !lower.contains("列表"))) {
            ctx.append("- ").append(toolService.getLatestOrderContext(userId)).append("\n");
            hasContext = true;
        }

        return hasContext ? ctx.toString() : "";
    }

    private boolean containsSensitiveWord(String msg) {
        for (String word : SENSITIVE_WORDS) {
            if (msg.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简单滑动窗口限流：1 分钟内最多 RATE_LIMIT_PER_MINUTE 次
     */
    private boolean checkRateLimit(Integer userId) {
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - TimeUnit.MINUTES.toMillis(1);

        ConcurrentLinkedDeque<Long> deque = rateLimitMap.computeIfAbsent(userId,
                k -> new ConcurrentLinkedDeque<>());

        // 清理过期记录
        while (!deque.isEmpty() && deque.peekFirst() < oneMinuteAgo) {
            deque.pollFirst();
        }

        if (deque.size() >= RATE_LIMIT_PER_MINUTE) {
            return false;
        }
        deque.addLast(now);
        return true;
    }

    /**
     * 流式接口：一次性发送完整文本并结束（用于降级场景）
     */
    private void sendEventAndComplete(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().data(text));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private String getMockReply(String userMsg) {
        String lowerMsg = userMsg.toLowerCase();

        if (lowerMsg.contains("注册") || lowerMsg.contains("登录") || lowerMsg.contains("密码")
                || lowerMsg.contains("手机") || lowerMsg.contains("头像")) {
            return "关于注册登录的问题，您可以点击页面右上角的登录按钮哦~";
        }
        if (lowerMsg.contains("发布") || lowerMsg.contains("商品") || lowerMsg.contains("上架")) {
            return "发布商品很简单的，点击首页的发布按钮就可以啦，提交后等待管理员审核哦~";
        }
        if (lowerMsg.contains("订单") || lowerMsg.contains("下单") || lowerMsg.contains("收货")) {
            return "订单相关问题，请在个人中心查看我的订单哦~";
        }
        if (lowerMsg.contains("充值") || lowerMsg.contains("支付") || lowerMsg.contains("付款")) {
            return "充值走支付宝沙箱环境，不会产生真实扣款哦~平台余额仅用于站内交易，暂不支持提现呢。";
        }
        if (lowerMsg.contains("桌宠") || lowerMsg.contains("皮肤") || lowerMsg.contains("形象")) {
            return "桌宠皮肤可以在个人中心切换哦，有很多可爱形象~";
        }
        if (lowerMsg.contains("余额") || lowerMsg.contains("多少钱")) {
            return "您的余额可以在个人中心查看哦~平台余额仅用于站内交易呢。";
        }

        if (isBusinessRelated(lowerMsg)) {
            Random random = new Random();
            return MOCK_REPLIES.get(random.nextInt(MOCK_REPLIES.size()));
        }

        return "我只解答本平台交易相关问题哦~";
    }

    private boolean isBusinessRelated(String msg) {
        String[] businessKeywords = {"交易", "购买", "出售", "账号", "游戏", "审核", "客服", "帮助"};
        for (String keyword : businessKeywords) {
            if (msg.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
