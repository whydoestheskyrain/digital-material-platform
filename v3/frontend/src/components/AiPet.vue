<template>
  <div
    class="pet-wrap"
    :style="{ left: `${petX}px`, top: `${petY}px` }"
    @mousedown="dragStart"
  >
    <img
      class="pet-img"
      :src="isClickAni && currentSkin.clickAni ? currentSkin.clickAni : currentSkin.idleAni"
      alt="AI客服桌宠"
      @click.stop="openChat"
    />

    <div v-if="chatShow" class="chat-overlay" @click="chatShow = false"></div>

    <div v-if="chatShow" class="chat-box" :class="{ minimized: chatMinimized }">
      <div class="chat-title">
        <span>✦ 客服机器人 ✦</span>
        <div class="title-actions">
          <span class="min-btn" @click="toggleMinimize" :title="chatMinimized ? '展开' : '最小化'">
            {{ chatMinimized ? '□' : '—' }}
          </span>
          <span class="close-btn" @click="closeChat" title="关闭">×</span>
        </div>
      </div>
      <div v-show="!chatMinimized" class="chat-body">
        <div class="chat-content" ref="chatContent">
          <div v-for="item in chatList" :key="item.id" :class="['chat-item', item.type]">
            <div class="chat-avatar">{{ item.type === 'user' ? '👤' : '👧' }}</div>
            <div class="chat-bubble">{{ item.text }}</div>
          </div>
          <div v-if="isLoading" class="loading-item">
            <div class="loading-dots">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
        <div class="chat-input">
          <input
            v-model="sendText"
            type="text"
            placeholder="输入你的问题..."
            @keyup.enter="sendChat"
            class="chat-input-field"
          />
          <button @click="sendChat" class="send-btn" :disabled="!sendText.trim() || isLoading">
            发送
          </button>
        </div>
      </div>
    </div>

    <button class="skin-btn" @click="skinDialog = true" title="更换形象">✦</button>
  </div>

  <div v-if="skinDialog" class="skin-overlay" @click="skinDialog = false">
    <div class="skin-dialog" @click.stop>
      <div class="skin-dialog-header">
        <span>选择桌宠形象</span>
        <span class="close-btn" @click="skinDialog = false">×</span>
      </div>
      <div class="skin-list">
        <div
          v-for="skin in skinList"
          :key="skin.id"
          :class="['skin-item', { active: currentSkin.id === skin.id }]"
          @click="selectSkin(skin)"
        >
          <img :src="skin.mainImg" :alt="skin.skinName" />
          <p>{{ skin.skinName }}</p>
          <span v-if="skin.isDefault === 1" class="default-tag">默认</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';
import { getCurrentAccount, getCurrentToken } from '@/utils/accountManager';

const petX = ref(1300);
const petY = ref(650);
const isDrag = ref(false);
const isClickAni = ref(false);
const chatShow = ref(false);
const chatMinimized = ref(false);
const skinDialog = ref(false);
const skinList = ref([]);
const currentSkin = ref({
  id: 1,
  skinName: '像素少女',
  idleAni: '/images/pet/pixel-girl-idle.svg',
  clickAni: '/images/pet/pixel-girl-click.svg',
  mainImg: '/images/pet/pixel-girl-idle.svg'
});
const chatList = ref([]);
const sendText = ref('');
const isLoading = ref(false);
const chatContent = ref(null);

// 多轮会话 ID：打开聊天时若为空则后端会生成，保持连续对话记忆
const sessionId = ref('');

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContent.value) {
      chatContent.value.scrollTop = chatContent.value.scrollHeight;
    }
  });
};

const initPet = async () => {
  const current = getCurrentAccount();
  if (current && current.user) {
    try {
      const res = await request.get('/pet/user/get', { params: { userId: current.user.id } });
      if (res.code === 200 && res.data) {
        petX.value = res.data.posX || 1300;
        petY.value = res.data.posY || 650;
        if (res.data.skin) {
          currentSkin.value = res.data.skin;
        }
      }
    } catch (e) {
      console.error('获取桌宠配置失败:', e);
    }
  }
  try {
    const skinRes = await request.get('/pet/skin/all');
    if (skinRes.code === 200 && skinRes.data) {
      skinList.value = skinRes.data;
      if (skinList.value.length > 0 && !currentSkin.value.id) {
        currentSkin.value = skinList.value.find(s => s.isDefault === 1) || skinList.value[0];
      }
    }
  } catch (e) {
    console.error('获取皮肤列表失败:', e);
  }
};
const dragStart = (e) => {
  isDrag.value = true;
  const startX = e.clientX;
  const startY = e.clientY;
  const originX = petX.value;
  const originY = petY.value;
  const moveThreshold = 5;
  let hasMoved = false;
  const move = (ev) => {
    if (!isDrag.value) return;
    const dx = Math.abs(ev.clientX - startX);
    const dy = Math.abs(ev.clientY - startY);
    if (dx > moveThreshold || dy > moveThreshold) {
      hasMoved = true;
    }
    petX.value = originX + (ev.clientX - startX);
    petY.value = originY + (ev.clientY - startY);
    petX.value = Math.max(0, Math.min(petX.value, window.innerWidth - 100));
    petY.value = Math.max(0, Math.min(petY.value, window.innerHeight - 150));
  };
  const dragEnd = async () => {
    isDrag.value = false;
    document.removeEventListener('mousemove', move);
    document.removeEventListener('mouseup', dragEnd);
    if (hasMoved) {
      const current = getCurrentAccount();
      if (current && current.user) {
        try {
          await request.post('/pet/user/save', {
            userId: current.user.id,
            skinId: currentSkin.value.id,
            posX: petX.value,
            posY: petY.value
          });
        } catch (e) {
          console.error('保存桌宠位置失败:', e);
        }
      }
    }
  };
  document.addEventListener('mousemove', move);
  document.addEventListener('mouseup', dragEnd);
};
const openChat = () => {
  if (isDrag.value) return;
  chatShow.value = true;
  chatMinimized.value = false;
  isClickAni.value = true;
  setTimeout(() => { isClickAni.value = false; }, 1500);
  scrollToBottom();
};
const toggleMinimize = () => {
  chatMinimized.value = !chatMinimized.value;
};
const closeChat = () => {
  chatShow.value = false;
  chatMinimized.value = false;
};

/**
 * 发送消息：优先走 SSE 流式接口（打字机效果），失败降级到普通接口。
 */
const sendChat = async () => {
  const text = sendText.value.trim();
  if (!text) return;
  const current = getCurrentAccount();
  if (!current || !current.user) {
    ElMessage.warning('请先登录后再使用AI客服哦~');
    return;
  }

  chatList.value.push({ id: Date.now(), type: 'user', text });
  sendText.value = '';
  isLoading.value = true;
  scrollToBottom();

  // 预先插入一条空 AI 消息，用于流式追加
  const aiMsgId = Date.now() + 1;
  chatList.value.push({ id: aiMsgId, type: 'ai', text: '' });

  try {
    const ok = await sendStreaming(text, aiMsgId);
    if (!ok) {
      // 流式失败，降级到普通接口
      await sendNormal(text, aiMsgId);
    }
  } catch (e) {
    console.error('发送消息失败:', e);
    chatList.value.find(m => m.id === aiMsgId).text = '网络连接异常，请稍后再试~';
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};

/**
 * 流式请求：fetch + ReadableStream 解析 SSE
 */
const sendStreaming = async (text, aiMsgId) => {
  const token = getCurrentToken();
  const url = `/pet/chat/stream?msg=${encodeURIComponent(text)}` +
    (sessionId.value ? `&sessionId=${sessionId.value}` : '');

  const resp = await fetch(url, {
    headers: token ? { 'Authorization': 'Bearer ' + token } : {}
  });

  if (!resp.ok || !resp.body) {
    return false;
  }

  const reader = resp.body.getReader();
  const decoder = new TextDecoder('utf-8');
  let buffer = '';
  const aiMsg = chatList.value.find(m => m.id === aiMsgId);

  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    // 解析 SSE：按 \n\n 分帧，每帧取 data: 后面的内容
    let idx;
    while ((idx = buffer.indexOf('\n\n')) !== -1) {
      const frame = buffer.slice(0, idx);
      buffer = buffer.slice(idx + 2);
      const dataLine = frame.split('\n').find(l => l.startsWith('data:'));
      if (dataLine) {
        const chunk = dataLine.slice(5).trim();
        if (chunk) {
          aiMsg.text += chunk;
          scrollToBottom();
        }
      }
    }
  }
  // 处理 buffer 中剩余的最后一帧
  if (buffer.trim()) {
    const dataLine = buffer.split('\n').find(l => l.startsWith('data:'));
    if (dataLine) {
      const chunk = dataLine.slice(5).trim();
      if (chunk) aiMsg.text += chunk;
    }
  }

  // 流式成功，sessionId 保持不变（首次由后端生成需回传，但流式接口不回传 sessionId，
  // 因此首次请求时前端自己生成一个，保证多轮连贯）
  if (!sessionId.value) {
    sessionId.value = generateSessionId();
  }
  return true;
};

/**
 * 降级：普通非流式接口
 */
const sendNormal = async (text, aiMsgId) => {
  const res = await request.post('/pet/chat', { msg: text, sessionId: sessionId.value });
  const aiMsg = chatList.value.find(m => m.id === aiMsgId);
  if (res.code === 200 && res.data) {
    aiMsg.text = res.data.reply || res.data;
    if (res.data.sessionId) {
      sessionId.value = res.data.sessionId;
    }
  } else {
    aiMsg.text = '抱歉，我暂时无法回答您的问题~';
  }
};

const generateSessionId = () => {
  return 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/x/g, () =>
    Math.floor(Math.random() * 16).toString(16)
  );
};

const selectSkin = async (skin) => {
  currentSkin.value = skin;
  skinDialog.value = false;
  const current = getCurrentAccount();
  if (current && current.user) {
    try {
      await request.post('/pet/user/save', {
        userId: current.user.id,
        skinId: skin.id,
        posX: petX.value,
        posY: petY.value
      });
    } catch (e) {
      console.error('保存皮肤失败:', e);
    }
  }
};
onMounted(() => {
  initPet();
});
</script>

<style scoped>
.pet-wrap {
  position: fixed;
  z-index: 9999;
  cursor: grab;
  transition: transform 0.1s ease;
}

.pet-wrap:active {
  cursor: grabbing;
}

.pet-img {
  width: 80px;
  height: 80px;
  object-fit: contain;
  filter: drop-shadow(0 4px 16px rgba(255,182,193,0.4));
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.chat-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  z-index: 9998;
}

.chat-box {
  position: absolute;
  right: 90px;
  bottom: 0;
  width: 320px;
  background: linear-gradient(180deg, #1d1622 0%, #16121a 100%);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(255,182,193,0.25), 0 0 60px rgba(173,216,230,0.15), 0 0 24px rgba(244,114,182,0.15);
  overflow: hidden;
  border: 1px solid rgba(244, 114, 182, 0.35);
  z-index: 9999;
}

.chat-box.minimized .chat-body {
  display: none;
}

.chat-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: linear-gradient(135deg, #FFB6C1 0%, #E6E6FA 50%, #B0E0E6 100%);
  color: #5D4E6D;
  font-weight: 600;
  font-size: 15px;
  position: relative;
}

.title-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.min-btn,
.close-btn {
  font-size: 20px;
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.3s;
  line-height: 1;
  user-select: none;
}

.min-btn:hover,
.close-btn:hover {
  opacity: 1;
}

.chat-content {
  height: 280px;
  overflow-y: auto;
  padding: 12px;
  background: transparent;
}

.chat-item {
  display: flex;
  margin-bottom: 14px;
  gap: 10px;
}

.chat-item.user {
  justify-content: flex-end;
}

.chat-item.user .chat-bubble {
  background: linear-gradient(135deg, #9370DB 0%, #8A2BE2 100%);
  color: white;
  border-radius: 14px 14px 4px 14px;
  box-shadow: 0 2px 10px rgba(147,112,219,0.3);
}

.chat-item.ai {
  justify-content: flex-start;
}

.chat-item.ai .chat-bubble {
  background: #26202b;
  color: var(--color-text-primary);
  border-radius: 14px 14px 14px 4px;
  box-shadow: 0 2px 10px rgba(255,182,193,0.3);
  border: 1px solid rgba(244, 114, 182, 0.25);
}

.chat-avatar {
  font-size: 26px;
  flex-shrink: 0;
}

.chat-bubble {
  max-width: 75%;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.5;
  position: relative;
}

.chat-item.ai .chat-bubble::before {
  content: '';
  position: absolute;
  left: -6px;
  top: 10px;
  width: 0;
  height: 0;
  border-top: 6px solid transparent;
  border-bottom: 6px solid transparent;
  border-right: 6px solid rgba(244, 114, 182, 0.25);
}

.loading-item {
  display: flex;
  justify-content: flex-start;
}

.loading-dots {
  display: flex;
  gap: 5px;
  padding: 10px 14px;
  background: #26202b;
  border-radius: 14px 14px 14px 4px;
  box-shadow: 0 2px 10px rgba(255,182,193,0.3);
  border: 1px solid rgba(244, 114, 182, 0.25);
}

.loading-dots span {
  width: 7px;
  height: 7px;
  background: linear-gradient(135deg, #FFB6C1, #B0E0E6);
  border-radius: 50%;
  animation: loading 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) { animation-delay: -0.32s; }
.loading-dots span:nth-child(2) { animation-delay: -0.16s; }

@keyframes loading {
  0%, 80%, 100% { transform: scale(0); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

.chat-input {
  display: flex;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid rgba(255,182,193,0.3);
  background: rgba(255,255,255,0.06);
}

.chat-input-field {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 22px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
  background: rgba(255,255,255,0.06);
  color: var(--color-text-primary);
}

.chat-input-field:focus {
  border-color: #FFB6C1;
  box-shadow: 0 0 0 3px rgba(255,182,193,0.2);
}

.send-btn {
  padding: 10px 22px;
  background: linear-gradient(135deg, #FFB6C1 0%, #B0E0E6 50%, #9370DB 100%);
  color: #5D4E6D;
  border: none;
  border-radius: 22px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
  box-shadow: 0 2px 10px rgba(255,182,193,0.3);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(255,182,193,0.4);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.skin-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, #FFB6C1, #B0E0E6);
  color: #5D4E6D;
  border: none;
  border-radius: 50%;
  font-size: 15px;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(255,182,193,0.5);
  transition: all 0.3s;
}

.skin-btn:hover {
  transform: scale(1.15);
  box-shadow: 0 4px 16px rgba(255,182,193,0.6);
}

.skin-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.skin-dialog {
  background: var(--color-bg-card);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  width: 420px;
  max-height: 520px;
  overflow: hidden;
  border: 1px solid rgba(255,182,193,0.4);
  box-shadow: 0 8px 32px rgba(255,182,193,0.2);
}

.skin-dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: transparent;
  border-bottom: 1px solid rgba(255,182,193,0.3);
  font-weight: 600;
  font-size: 16px;
  color: var(--color-text-primary);
}

.skin-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  padding: 16px;
  max-height: 380px;
  overflow-y: auto;
  background: transparent;
}

.skin-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px;
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  background: rgba(255, 255, 255, 0.05);
}

.skin-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255,182,193,0.2);
}

.skin-item.active {
  border-color: #FFB6C1;
  background: rgba(244, 114, 182, 0.18);
  box-shadow: 0 4px 12px rgba(255,182,193,0.3);
}

.skin-item img {
  width: 80px;
  height: 80px;
  object-fit: contain;
  margin-bottom: 8px;
  filter: drop-shadow(0 2px 8px rgba(0,0,0,0.1));
}

.skin-item p {
  font-size: 13px;
  color: var(--color-text-primary);
  margin: 0;
  font-weight: 500;
}

.default-tag {
  position: absolute;
  top: 4px;
  right: 4px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #FFB6C1, #B0E0E6);
  color: #5D4E6D;
  font-size: 10px;
  border-radius: 4px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .pet-wrap {
    left: 20px !important;
    right: auto !important;
  }
  
  .chat-box {
    right: auto;
    left: 90px;
    width: 280px;
  }
  
  .skin-dialog {
    width: 90%;
    max-width: 320px;
  }
  
  .skin-list {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>