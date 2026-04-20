import http from 'node:http'
import https from 'node:https'
import path from 'path'

import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'

const backendTarget = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

const createSessionListBridge = (): Plugin => ({
  name: 'session-list-bridge',
  configureServer(server) {
    server.middlewares.use('/api/session/list', (req, res, next) => {
      if (req.method !== 'GET') {
        next()
        return
      }

      const requestUrl = new URL(req.originalUrl || req.url || '/api/session/list', 'http://localhost')
      const payload = JSON.stringify({
        page: Number(requestUrl.searchParams.get('page') || 1),
        pageSize: Number(requestUrl.searchParams.get('pageSize') || 10),
        beginDate: requestUrl.searchParams.get('beginDate') || undefined,
        endDate: requestUrl.searchParams.get('endDate') || undefined
      })

      const targetUrl = new URL('/session/list', backendTarget)
      const transport = targetUrl.protocol === 'https:' ? https : http

      const proxyReq = transport.request(
        {
          hostname: targetUrl.hostname,
          port: targetUrl.port,
          path: targetUrl.pathname,
          method: 'GET',
          headers: {
            'content-type': 'application/json',
            'content-length': Buffer.byteLength(payload),
            token: req.headers.token || ''
          }
        },
        (proxyRes) => {
          res.statusCode = proxyRes.statusCode || 500

          const contentType = proxyRes.headers['content-type']
          if (contentType) {
            res.setHeader('content-type', contentType)
          }

          let body = ''
          proxyRes.setEncoding('utf8')
          proxyRes.on('data', (chunk) => {
            body += chunk
          })
          proxyRes.on('end', () => {
            res.end(body)
          })
        }
      )

      proxyReq.on('error', (error) => {
        res.statusCode = 502
        res.setHeader('content-type', 'application/json; charset=utf-8')
        res.end(
          JSON.stringify({
            code: 0,
            msg: `session/list proxy failed: ${error.message}`,
            data: null
          })
        )
      })

      proxyReq.write(payload)
      proxyReq.end()
    })
  }
})

export default defineConfig({
  plugins: [vue(), createSessionListBridge()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: backendTarget,
        changeOrigin: true,
        rewrite: (value) => value.replace(/^\/api/, '')
      }
    }
  }
})
