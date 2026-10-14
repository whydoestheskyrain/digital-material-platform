const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  publicPath: './',
  transpileDependencies: true,
  devServer: {
    port: 8082,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/product': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/user': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/gameType': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/order': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/transaction': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/recharge': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/alipay': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/pet': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/feedback': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/notification': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/admin': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        ws: true
      }
    },
    client: {
      overlay: false
    }
  }
})
