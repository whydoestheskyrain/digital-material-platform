import { createRouter, createWebHashHistory} from 'vue-router'
import ProductList from '@/views/ProductList.vue'
import ProductAdd from '@/views/ProductAdd.vue'
import ProductEdit from '@/views/ProductEdit.vue'
import ProductDetail from '@/views/ProductDetail.vue'
import UserList from '@/views/UserList.vue'
import UserAdd from '@/views/UserAdd.vue'
import UserEdit from '@/views/UserEdit.vue'
import OrderList from '@/views/OrderList.vue'
import OrderDetail from '@/views/OrderDetail.vue'
import OrderAdd from '@/views/OrderAdd.vue'
import OrderEdit from '@/views/OrderEdit.vue'
import TransactionList from '@/views/TransactionList.vue'
import TransactionAdd from '@/views/TransactionAdd.vue'
import TransactionEdit from '@/views/TransactionEdit.vue'
import UserLogin from '@/views/UserLogin.vue'
import HomeView from '../views/HomeView.vue'
import UserRegister from '@/views/UserRegister.vue'
import AdminView from '@/views/AdminView.vue'
import AdminUser from '@/views/AdminUser.vue'
import AdminProduct from '@/views/AdminProduct.vue'
import AdminOrder from '@/views/AdminOrder.vue'
import AdminGameType from '@/views/AdminGameType.vue'
import AdminFeedback from '@/views/AdminFeedback.vue'
import AdminPetSkin from '@/views/AdminPetSkin.vue'
import UserCenter from '../views/UserCenter.vue'
import NotificationView from '../views/NotificationView.vue'

const routes = [
  
{ path: '/user/center', component: UserCenter },
  { path: '/user/notifications', component: NotificationView },

  {
    path: '/admin/order',
    name: 'AdminOrder',
    component: AdminOrder,
    meta: { requiresAuth: true, isAdmin: true }
  },
      {
    path: '/order/my',
    name: 'MyOrder',
    component: OrderList
  },
  {
    path: '/admin',
    redirect: '/login'
  },
  {
    path: '/admin/home',
    name: 'AdminHome',
    component: AdminView
  },
  {
    path: '/admin/user',
    name: 'AdminUser',
    component: AdminUser
  },
  {
    path: '/admin/product',
    name: 'AdminProduct',
    component: AdminProduct
  },
  {
    path: '/admin/gameType',
    name: 'AdminGameType',
    component: AdminGameType,
    meta: { requiresAuth: true, isAdmin: true }
  },
  {
    path: '/admin/feedback',
    name: 'AdminFeedback',
    component: AdminFeedback,
    meta: { requiresAuth: true, isAdmin: true }
  },
  {
    path: '/admin/petSkin',
    name: 'AdminPetSkin',
    component: AdminPetSkin,
    meta: { requiresAuth: true, isAdmin: true }
  },
  
  {
  path: '/register',
  name: 'UserRegister',
  component: UserRegister
},
  {
    path: '/',
    name: 'home',
    component: HomeView
  },

  {
    path: '/login',
    name: 'UserLogin',
    component: UserLogin
  },

  { path: '/product', redirect: '/product/list' },
  { path: '/product/list', component: ProductList },
  { path: '/product/add', component: ProductAdd },
  { path: '/product/edit/:id', component: ProductEdit },
  { path: '/product/detail/:id', component: ProductDetail },

  { path: '/user', redirect: '/user/list' },
  { path: '/user/list', component: UserList },
  { path: '/user/add', component: UserAdd },
  { path: '/user/edit/:id', component: UserEdit },

  { path: '/order', redirect: '/order/list' },
  { path: '/order/list', component: OrderList },
  { path: '/order/detail/:id', component: OrderDetail },
  { path: '/order/add', component: OrderAdd },
  { path: '/order/edit/:id', component: OrderEdit },

  { path: '/transaction', redirect: '/transaction/list' },
  { path: '/transaction/list', component: TransactionList },
  { path: '/transaction/add', component: TransactionAdd },
  { path: '/transaction/edit/:id', component: TransactionEdit },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
