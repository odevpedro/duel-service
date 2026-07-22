import initSqlJs from 'sql.js'
import wasmUrl from 'sql.js/dist/sql-wasm.wasm?url'

let databasePromise

function openDatabase() {
  if (!databasePromise) {
    databasePromise = Promise.all([
      initSqlJs({ locateFile: () => wasmUrl }),
      fetch('/local-assets/cards.cdb').then(response => {
        if (!response.ok) throw new Error('cards.cdb local indisponivel')
        return response.arrayBuffer()
      }),
    ]).then(([SQL, bytes]) => new SQL.Database(new Uint8Array(bytes)))
  }
  return databasePromise
}

const cache = new Map()

function unknownCard(code) {
  return {
    id: Number(code) || 0,
    name: code ? `Carta ${code}` : 'Carta oculta',
    description: '',
    strings: [],
  }
}

export async function getCard(code) {
  const id = Number(code) || 0
  if (!id) return unknownCard(0)
  if (cache.has(id)) return cache.get(id)

  const database = await openDatabase()
  const statement = database.prepare(`
    SELECT texts.id, texts.name, texts.desc,
           texts.str1, texts.str2, texts.str3, texts.str4,
           texts.str5, texts.str6, texts.str7, texts.str8,
           texts.str9, texts.str10, texts.str11, texts.str12,
           texts.str13, texts.str14, texts.str15, texts.str16,
           datas.type, datas.atk, datas.def, datas.level, datas.attribute, datas.race
      FROM texts
      JOIN datas ON datas.id = texts.id
     WHERE texts.id = ?
  `)
  statement.bind([id])
  const card = statement.step() ? statement.getAsObject() : null
  statement.free()

  const value = card ? {
    id,
    name: card.name,
    description: card.desc,
    strings: Array.from({ length: 16 }, (_, index) => card[`str${index + 1}`] || ''),
    type: card.type,
    attack: card.atk,
    defense: card.def,
    level: card.level & 0xff,
    attribute: card.attribute,
    race: card.race,
  } : unknownCard(id)
  cache.set(id, value)
  return value
}

export async function preloadCards(codes) {
  await Promise.all([...new Set(codes.filter(Boolean))].map(getCard))
}

export async function resolveDescription(value) {
  const description = Number(value) || 0
  if (description > 0xffffffff) {
    const code = Math.floor(description / 0x10000)
    const index = description % 0x10000
    const card = await getCard(code)
    return card.strings[index] || `${card.name}: efeito ${index + 1}`
  }
  if (description >= 10_000) {
    const code = Math.floor(description / 16)
    const index = description & 0xf
    const card = await getCard(code)
    return card.strings[index] || `${card.name}: efeito ${index + 1}`
  }
  return SYSTEM_STRINGS[description] || `Opcao ${description}`
}

const SYSTEM_STRINGS = {
  30: 'Ativar',
  31: 'Invocar',
  32: 'Invocar por Flip',
  33: 'Invocar por Invocacao-Especial',
  34: 'Baixar',
  35: 'Posicao de Ataque',
  36: 'Posicao de Defesa',
  37: 'Atacar',
  38: 'Ataque direto',
  40: 'Selecionar carta',
  41: 'Selecionar alvo',
  92: 'Sim',
  93: 'Nao',
}
