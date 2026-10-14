<template>
  <div class="page-wrapper">
    <div class="container">
      <div class="form-wrapper">
        <!-- 顶部标题区域 -->
        <div class="form-header">
          <div class="form-title-section">
            <div class="title-icon-wrapper">
              <span class="title-icon">📤</span>
            </div>
            <div class="title-text">
              <h1 class="form-title">发布商品</h1>
              <p class="form-tip">填写游戏账号信息，审核通过后将在首页展示</p>
            </div>
          </div>
          
          <!-- 步骤指示器 -->
          <div class="form-steps">
            <div class="step-item" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
              <div class="step-indicator">
                <span class="step-number">1</span>
                <span class="step-check" v-if="currentStep > 1">✓</span>
              </div>
              <span class="step-text">填写信息</span>
            </div>
            <div class="step-line" :class="{ active: currentStep > 1 }"></div>
            <div class="step-item" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
              <div class="step-indicator">
                <span class="step-number">2</span>
                <span class="step-check" v-if="currentStep > 2">✓</span>
              </div>
              <span class="step-text">提交审核</span>
            </div>
            <div class="step-line" :class="{ active: currentStep > 2 }"></div>
            <div class="step-item" :class="{ active: currentStep >= 3 }">
              <div class="step-indicator">
                <span class="step-number">3</span>
                <span class="step-check" v-if="currentStep > 3">✓</span>
              </div>
              <span class="step-text">审核结果</span>
            </div>
          </div>
        </div>

        <el-form :model="form" label-width="120px" class="product-form" :rules="formRules" ref="formRef">
          <div class="form-section">
            <h3 class="section-title">
              <el-icon class="section-icon"><Document /></el-icon> 基本信息
            </h3>
            <el-form-item label="游戏类型" prop="gameTypeId">
              <el-select
                v-model="form.gameTypeId"
                placeholder="请选择游戏类型"
                class="form-input"
              >
                <el-option
                  v-for="type in gameTypes"
                  :key="type.id"
                  :label="type.typeName"
                  :value="type.id"
                >
                  <span>{{ type.icon || '🎮' }} {{ type.typeName }}</span>
                </el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="商品标题" prop="title">
              <el-input
                v-model="form.title"
                placeholder="如：王者荣耀钻石段位账号"
                class="form-input"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="游戏区服">
              <el-input
                v-model="form.serverName"
                placeholder="如：官服/微信区/QQ区"
                class="form-input"
              />
            </el-form-item>

            <el-form-item label="角色等级">
              <el-input-number v-model="form.roleLevel" :min="0" :max="999" placeholder="请输入游戏等级" />
            </el-form-item>

            <el-form-item label="商品价格" prop="price">
              <el-input
                v-model.number="form.price"
                type="number"
                placeholder="请输入售价"
                class="form-input price-input"
              >
                <template #prefix>¥</template>
              </el-input>
            </el-form-item>

            <el-form-item label="商品描述">
              <textarea
                v-model="form.desc"
                rows="4"
                placeholder="简要描述账号情况（可选），有助于买家了解商品"
                class="product-textarea"
              ></textarea>
            </el-form-item>

            <!-- 商品图片上传 -->
            <el-form-item label="商品图片">
              <el-upload
                action="/api/upload/image"
                :on-success="handleUploadSuccess"
                :before-upload="beforeUpload"
                :on-remove="handleRemove"
                list-type="picture-card"
                :limit="1"
                :file-list="fileList"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              <div class="upload-tip">支持 jpg/png，不超过2MB。不传则使用默认图片</div>
            </el-form-item>

            <!-- 商品详情描述 -->
            <el-form-item label="商品详情">
              <textarea
                v-model="form.detailDesc"
                rows="6"
                placeholder="详细描述商品信息，如：英雄数量、皮肤数量、段位、亮点等，帮助买家更好地了解商品"
                class="product-textarea"
              ></textarea>
            </el-form-item>

            <!-- 详情图片上传（多张） -->
            <el-form-item label="详情图片">
              <el-upload
                action="/api/upload/image"
                :on-success="handleDetailImageSuccess"
                :before-upload="beforeUpload"
                :on-remove="handleDetailImageRemove"
                list-type="picture-card"
                :limit="9"
                :file-list="detailFileList"
                multiple
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              <div class="upload-tip">可上传多张商品截图，最多9张，支持 jpg/png，不超过2MB</div>
            </el-form-item>
          </div>

          <div class="form-section account-section">
            <h3 class="section-title">
              <el-icon class="section-icon"><Lock /></el-icon> 账号信息
              <span class="section-tag">仅管理员可见</span>
            </h3>
            <p class="account-tip">以下信息仅用于交易核实，管理员会严格保密</p>

            <el-form-item label="游戏账号" prop="accountUsername">
              <el-input
                v-model="form.accountUsername"
                placeholder="请输入游戏登录账号"
                class="form-input"
              >
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="游戏密码" prop="accountPassword">
              <el-input
                v-model="form.accountPassword"
                type="password"
                placeholder="请输入游戏登录密码"
                class="form-input"
                show-password
              >
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="绑定信息">
              <el-input
                v-model="form.bindInfo"
                placeholder="如：绑定的手机/邮箱（可选）"
                class="form-input"
              >
                <template #prefix><el-icon><Iphone /></el-icon></template>
              </el-input>
            </el-form-item>
          </div>

          <div class="form-actions">
            <el-button @click="back" class="btn-back">
              <span>✕</span> 取消
            </el-button>
            <el-button type="primary" @click="submit" class="btn-submit" :loading="submitting">
              <span v-if="!submitting">📤</span>
              {{ submitting ? '提交中...' : '提交发布' }}
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getCurrentUser } from '@/utils/accountManager'

const router = useRouter()

const user = getCurrentUser()
const gameTypes = ref([])
const formRef = ref(null)
const submitting = ref(false)
const currentStep = ref(1)
const fileList = ref([])
const detailFileList = ref([])

const form = ref({
  sellerId: user?.id || 1,
  gameTypeId: '',
  title: '',
  serverName: '',
  roleLevel: 0,
  price: 0,
  desc: '',
  detailDesc: '',
  detailImages: '',
  status: 0,
  accountUsername: '',
  accountPassword: '',
  bindInfo: '',
  imageUrl: ''   // 存储图片URL
})

const formRules = {
  gameTypeId: [{ required: true, message: '请选择游戏类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入商品标题', trigger: 'blur' }],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '价格必须大于0', trigger: 'blur' }
  ],
  accountUsername: [{ required: true, message: '请输入游戏账号', trigger: 'blur' }],
  accountPassword: [{ required: true, message: '请输入游戏密码', trigger: 'blur' }]
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

const handleUploadSuccess = (res, file) => {
  if (res.code === 200) {
    form.value.imageUrl = res.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(res.msg || '上传失败')
    fileList.value = fileList.value.filter(f => f.uid !== file.uid)
  }
}

const handleRemove = () => {
  form.value.imageUrl = ''
}

const handleDetailImageSuccess = (res, file) => {
  if (res.code === 200) {
    const images = form.value.detailImages ? form.value.detailImages.split(',') : []
    images.push(res.data)
    form.value.detailImages = images.join(',')
    ElMessage.success('详情图片上传成功')
  } else {
    ElMessage.error(res.msg || '上传失败')
    detailFileList.value = detailFileList.value.filter(f => f.uid !== file.uid)
  }
}

const handleDetailImageRemove = (file) => {
  const images = form.value.detailImages ? form.value.detailImages.split(',') : []
  const url = file.response?.data || file.url
  form.value.detailImages = images.filter(img => img !== url).join(',')
}

const loadGameTypes = async () => {
  try {
    const res = await request.get('/gameType/enabled')
    gameTypes.value = res.data || []
  } catch (err) {
    ElMessage.error('获取游戏类型失败')
  }
}

const submit = async () => {
  try {
    await formRef.value.validate()

    if (!form.value.gameTypeId) {
      ElMessage.warning('请选择游戏类型')
      return
    }
    if (!form.value.title) {
      ElMessage.warning('请输入商品标题')
      return
    }
    if (!form.value.price || form.value.price <= 0) {
      ElMessage.warning('请输入有效价格')
      return
    }
    if (!form.value.accountUsername) {
      ElMessage.warning('请输入游戏账号')
      return
    }
    if (!form.value.accountPassword) {
      ElMessage.warning('请输入游戏密码')
      return
    }

    submitting.value = true
    currentStep.value = 2

    const selectedGameType = gameTypes.value.find(t => t.id === form.value.gameTypeId)
    const submitData = {
      ...form.value,
      gameTypeId: Number(form.value.gameTypeId),
      gameType: selectedGameType?.typeName || '',
      description: form.value.desc
    }
    await request.post('/product/add', submitData)

    currentStep.value = 3
    ElMessage.success('提交成功！商品待审核后将在首页展示')
    setTimeout(() => {
      router.push('/product/list')
    }, 1500)
  } catch (err) {
    console.error('提交错误：', err)
    ElMessage.error('提交失败，请稍后重试')
    currentStep.value = 1
  } finally {
    submitting.value = false
  }
}

const back = () => {
  router.push('/product/list')
}

onMounted(() => {
  loadGameTypes()
})
</script>

<style scoped>
.form-wrapper {
  max-width: 900px;
  width: 95%;
  margin: 0 auto;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 24px;
  background: var(--color-primary-gradient);
  border-radius: 16px;
  color: white;
  /* 长表单滚动时保持上下文：标题与步骤指示器吸顶 */
  position: sticky;
  top: 0;
  z-index: 50;
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.25);
}

.form-title-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-icon-wrapper {
  width: 56px;
  height: 56px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.title-icon {
  font-size: 28px;
}

.title-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: white;
}

.form-tip {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
  color: rgba(255, 255, 255, 0.85);
}

.form-steps {
  display: flex;
  align-items: center;
  gap: 0;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.step-indicator {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.step-item.active .step-indicator {
  background: var(--color-bg-card-hover);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.step-item.completed .step-indicator {
  background: #67c23a;
}

.step-number {
  font-size: 15px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
}

.step-item.active .step-number {
  color: var(--color-link);
}

.step-item.completed .step-number {
  display: none;
}

.step-check {
  display: none;
  font-size: 16px;
  color: white;
}

.step-item.completed .step-check {
  display: block;
}

.step-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
  transition: all 0.3s ease;
}

.step-item.active .step-text {
  color: white;
  font-weight: 600;
}

.step-line {
  width: 40px;
  height: 3px;
  background: rgba(255, 255, 255, 0.3);
  margin: 0 8px;
  transition: all 0.3s ease;
}

.step-line.active {
  background: rgba(255, 255, 255, 0.9);
}

.product-form {
  max-width: 100%;
}

.form-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 28px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.account-section {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid #ffeeba;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 24px 0;
}

.section-icon {
  font-size: 20px;
  color: var(--color-primary);
}

.section-tag {
  font-size: 11px;
  padding: 3px 10px;
  background: #f56c6c;
  color: white;
  border-radius: 12px;
  margin-left: 10px;
  font-weight: normal;
}

.form-input {
  width: 100%;
  border-radius: 10px;
  z-index: 1;
  position: relative;
}

.product-textarea {
  width: 100%;
  min-height: 120px;
  padding: 12px;
  background: var(--color-bg-card-hover);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.product-textarea:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(124, 58, 237, 0.2);
}

.product-textarea::placeholder {
  color: var(--color-text-secondary);
}

.price-input {
  max-width: 220px;
}

.account-tip {
  color: #e6a23c;
  font-size: 13px;
  margin: -10px 0 20px 0;
  padding: 12px 16px;
  background: rgba(230, 162, 60, 0.1);
  border-radius: 8px;
}

.form-actions {
  display: flex;
  gap: 16px;
  margin-top: 30px;
}

.btn-submit {
  flex: 2;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  background: var(--color-primary-gradient);
  border: none;
  box-shadow: 0 6px 20px rgba(124, 58, 237, 0.4);
  transition: all 0.3s ease;
}

.btn-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(124, 58, 237, 0.5);
  background: var(--color-primary-gradient-hover);
}

.btn-back {
  flex: 1;
  height: 48px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid var(--color-border);
  color: var(--color-text-regular);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.btn-back:hover {
  background: rgba(0, 0, 0, 0.08);
  border-color: var(--color-border);
  transform: translateY(-2px);
}

.upload-tip {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 8px;
}

@media (max-width: 768px) {
  .form-header {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .form-title-section {
    flex-direction: column;
  }

  .form-steps {
    width: 100%;
    justify-content: center;
  }

  .step-line {
    width: 30px;
  }

  .form-actions {
    flex-direction: column;
  }

  .form-section {
    padding: 16px 12px;
  }

  .form-container {
    padding: 16px;
  }

  .el-form-item__label {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .form-section {
    padding: 12px 10px;
  }
  .step-text {
    font-size: 11px;
  }
  .form-actions .el-button {
    width: 100%;
  }
}
</style>