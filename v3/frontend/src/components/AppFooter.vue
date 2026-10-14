<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <span class="footer-logo"></span>
        <span class="footer-name">虚拟交易平台</span>
      </div>

      <div class="footer-notice">
        <p class="notice-line"> 本站为演示项目，平台内所有商品、订单、资金均为虚拟数据，不提供任何真实交易服务</p>
        <p class="notice-line">支付功能基于支付宝沙箱环境，不会产生真实扣款；请勿在站内使用真实账号、密码或支付信息</p>
      </div>

      <div class="footer-demo">
        演示账号：admin / user1 / user2（密码均为 123456）
      </div>

      <p class="copyright">© 2026 虚拟交易平台 · 仅供学习演示使用</p>
    </div>
  </footer>
</template>

<script setup>
// 全局页尾：纯展示组件，无逻辑
</script>

<style scoped>
.app-footer {
  background: #555;
  border-top: 1px solid #4a4a4a;
  padding: 32px 16px;
  margin-top: 60px;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}

.footer-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 16px;
}

.footer-logo {
  font-size: 26px;
}

.footer-name {
  font-size: 17px;
  font-weight: bold;
  color: #fff;
}

.footer-notice {
  margin-bottom: 12px;
}

.notice-line {
  color: #f1f1f1;
  font-size: 13px;
  line-height: 1.8;
  margin: 0;
}

.footer-demo {
  color: #ddd;
  font-size: 13px;
  margin-bottom: 12px;
}

.copyright {
  color: #bbb;
  font-size: 12px;
  margin: 0;
}

@media (max-width: 768px) {
  .app-footer {
    padding: 24px 12px;
    margin-top: 40px;
  }

  .notice-line {
    font-size: 12px;
  }
}
</style>
