import fs from 'node:fs'
import path from 'node:path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const repoRoot = path.resolve(import.meta.dirname, '../..')
const cardsDatabase = path.join(repoRoot, '.local-runtime/resources/BabelCDB/cards.cdb')
const imageCache = path.join(repoRoot, '.local-runtime/card-images')

function localCardAssets() {
  return {
    name: 'local-card-assets',
    configureServer(server) {
      server.middlewares.use('/local-assets/cards.cdb', (_req, res) => {
        if (!fs.existsSync(cardsDatabase)) {
          res.statusCode = 503
          res.end('cards.cdb ausente; execute ./dev.sh runtime-setup')
          return
        }
        res.setHeader('Content-Type', 'application/vnd.sqlite3')
        fs.createReadStream(cardsDatabase).pipe(res)
      })

      server.middlewares.use('/local-assets/cards', async (req, res) => {
        const code = path.basename(req.url || '').replace(/\.jpg$/i, '')
        if (!/^\d{1,10}$/.test(code)) {
          res.statusCode = 400
          res.end()
          return
        }

        fs.mkdirSync(imageCache, { recursive: true })
        const cached = path.join(imageCache, `${code}.jpg`)
        if (!fs.existsSync(cached)) {
          try {
            const upstream = await fetch(`https://images.ygoprodeck.com/images/cards/${code}.jpg`)
            if (!upstream.ok) throw new Error(`HTTP ${upstream.status}`)
            fs.writeFileSync(cached, Buffer.from(await upstream.arrayBuffer()))
          } catch {
            res.statusCode = 404
            res.end()
            return
          }
        }

        res.setHeader('Content-Type', 'image/jpeg')
        res.setHeader('Cache-Control', 'public, max-age=31536000, immutable')
        fs.createReadStream(cached).pipe(res)
      })
    },
  }
}

export default defineConfig({
  plugins: [react(), localCardAssets()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/evolution': {
        target: 'http://127.0.0.1:7922',
        changeOrigin: true,
        rewrite: value => value.replace(/^\/evolution/, ''),
      },
      '/windbot': {
        target: 'http://127.0.0.1:2399',
        changeOrigin: true,
        rewrite: value => value.replace(/^\/windbot/, ''),
      },
    },
  },
})
