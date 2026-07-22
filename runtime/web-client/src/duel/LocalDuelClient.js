import YGOProDeck from 'ygopro-deck-encode'
import * as Y from 'ygopro-msg-encode'
import blueEyesYdk from '../../../decks/AI_BE2025.ydk?raw'
import { parseModernUpdateCard, parseModernUpdateData } from './modernQuery'
import { parseModernGameMessage } from './modernPrompt'

const PHASE_NAMES = {
  1: 'DRAW',
  2: 'STANDBY',
  4: 'MAIN 1',
  8: 'BATTLE',
  16: 'MAIN 2',
  32: 'END',
}

export const LOCATIONS = {
  DECK: 1,
  HAND: 2,
  MZONE: 4,
  SZONE: 8,
  GRAVE: 16,
  REMOVED: 32,
  EXTRA: 64,
  OVERLAY: 128,
}

function parseDeck(source) {
  const result = { main: [], extra: [], side: [] }
  let section = 'main'
  for (const rawLine of source.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (line === '#extra') section = 'extra'
    else if (line === '!side') section = 'side'
    else if (/^\d+$/.test(line)) result[section].push(Number(line))
  }
  return result
}

export const BLUE_EYES_DECK = parseDeck(blueEyesYdk)

function emptyState() {
  return {
    status: 'idle',
    statusText: 'Runtime local pronto',
    room: null,
    players: ['', ''],
    localPlayer: 0,
    isHost: false,
    duelStarted: false,
    lp: [8000, 8000],
    turn: 0,
    turnPlayer: 0,
    phase: '',
    zones: {},
    deckCounts: [40, 40],
    extraCounts: [15, 15],
    prompt: null,
    winner: null,
    log: [],
  }
}

function zoneKey(player, location) {
  return `${player}:${location}`
}

function normalizeCard(query, player, location, sequence) {
  if (!query || query.empty) return null
  return {
    ...query,
    code: Number(query.code) || 0,
    controller: query.controller ?? player,
    location: query.location ?? location,
    sequence: query.sequence ?? sequence,
    position: query.position ?? 0,
  }
}

export class LocalDuelClient {
  constructor(onChange) {
    this.onChange = onChange
    this.state = emptyState()
    this.socket = null
    this.buffer = new Uint8Array(0)
    this.botLaunched = false
    this.startTimer = null
  }

  snapshot() {
    return this.state
  }

  emit(patch = {}) {
    this.state = { ...this.state, ...patch }
    this.onChange(this.state)
  }

  addLog(text) {
    const log = [...this.state.log, text].slice(-80)
    this.emit({ log })
  }

  async start(playerName = 'Duelista') {
    this.disconnect()
    this.state = emptyState()
    this.emit({ status: 'creating', statusText: 'Criando sala nativa...' })

    const roomName = `Local Blue-Eyes ${Date.now().toString().slice(-6)}`
    const response = await fetch('/evolution/api/room', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: roomName,
        mode: 0,
        bestOf: 1,
        rule: 0,
        banlist: '2026.07 OCG',
        teamQuantity: 1,
        isRanked: false,
      }),
    })
    if (!response.ok) throw new Error(`Falha ao criar sala: HTTP ${response.status}`)
    const { password } = await response.json()
    const room = await this.findRoom(roomName, password)
    this.emit({ room, status: 'connecting', statusText: 'Conectando ao servidor EDOPro...' })
    await this.connect(playerName, room)
  }

  async findRoom(name, password) {
    for (let attempt = 0; attempt < 30; attempt += 1) {
      const response = await fetch('/evolution/api/getrooms')
      if (response.ok) {
        const payload = await response.json()
        const room = payload.rooms?.find(candidate => candidate.roomname === name)
        if (room) return { ...room, password }
      }
      await new Promise(resolve => setTimeout(resolve, 100))
    }
    throw new Error('A sala foi criada, mas nao apareceu no servidor')
  }

  connect(playerName, room) {
    return new Promise((resolve, reject) => {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const socket = new WebSocket(`${protocol}//${window.location.hostname}:4001`)
      socket.binaryType = 'arraybuffer'
      this.socket = socket

      socket.onopen = () => {
        const info = new Y.YGOProCtosPlayerInfo()
        info.name = playerName.slice(0, 20)
        this.send(info)

        this.sendRaw(createJoinGameFrame(room.roomid, room.password))
        this.emit({ status: 'lobby', statusText: 'Entrando na sala...' })
        resolve()
      }
      socket.onerror = () => reject(new Error('Nao foi possivel abrir o WebSocket local'))
      socket.onclose = event => {
        if (this.state.winner === null && this.state.status !== 'idle') {
          this.emit({ status: 'closed', statusText: `Conexao encerrada (${event.code})` })
        }
      }
      socket.onmessage = event => this.pushBytes(new Uint8Array(event.data))
    })
  }

  pushBytes(bytes) {
    const merged = new Uint8Array(this.buffer.length + bytes.length)
    merged.set(this.buffer)
    merged.set(bytes, this.buffer.length)
    this.buffer = merged

    let offset = 0
    while (offset + 2 <= this.buffer.length) {
      const payloadLength = this.buffer[offset] | (this.buffer[offset + 1] << 8)
      const frameLength = payloadLength + 2
      if (offset + frameLength > this.buffer.length) break
      this.handleFrame(this.buffer.slice(offset, offset + frameLength))
      offset += frameLength
    }
    this.buffer = this.buffer.slice(offset)
  }

  handleFrame(frame) {
    if (frame[2] === 0x17) {
      this.addLog('Replay recebido')
      return
    }
    if (frame[2] === 0x01 && frame[3] === 0x06) {
      const update = parseModernUpdateData(frame)
      this.addLog('UpdateData')
      this.replaceZone(update.player, update.location, update.cards)
      return
    }
    if (frame[2] === 0x01 && frame[3] === 0x07) {
      const update = parseModernUpdateCard(frame)
      this.addLog('UpdateCard')
      this.updateCard(update.player, update.location, update.sequence, update.card)
      return
    }

    try {
      const modernMessage = parseModernGameMessage(frame)
      if (modernMessage) {
        this.handleGameMessage(modernMessage)
        return
      }
    } catch (error) {
      this.addLog(`Prompt moderno ignorado: ${error.message}`)
      return
    }

    let message
    try {
      message = Y.YGOProStoc.getInstanceFromPayload(frame)
    } catch (error) {
      this.addLog(`Pacote ignorado ${frame[2]}:${frame[3]}: ${error.message}`)
      return
    }
    if (!message) return

    if (message instanceof Y.YGOProStocJoinGame) {
      this.emit({ statusText: 'Sala conectada; enviando deck Blue-Eyes...' })
      this.uploadDeck()
    } else if (message instanceof Y.YGOProStocTypeChange) {
      const localPlayer = message.playerPosition
      this.emit({ localPlayer, isHost: message.isHost })
      if (message.isHost) void this.launchBot()
    } else if (message instanceof Y.YGOProStocHsPlayerEnter) {
      const players = [...this.state.players]
      players[message.pos] = message.name.replace(/\0/g, '').trim()
      this.emit({ players })
      this.scheduleStart()
    } else if (message instanceof Y.YGOProStocHsPlayerChange) {
      this.scheduleStart()
    } else if (message instanceof Y.YGOProStocDuelStart) {
      this.emit({ status: 'dueling', statusText: 'Duelo iniciado pelo ocgcore', duelStarted: true })
    } else if (message instanceof Y.YGOProStocSelectHand) {
      this.emit({ prompt: { type: 'rps' } })
    } else if (message instanceof Y.YGOProStocSelectTp) {
      this.emit({ prompt: { type: 'turn-order' } })
    } else if (message instanceof Y.YGOProStocHandResult) {
      this.addLog(`Pedra-papel-tesoura: ${message.res1} x ${message.res2}`)
    } else if (message instanceof Y.YGOProStocTimeLimit) {
      this.send(new Y.YGOProCtosTimeConfirm())
    } else if (message instanceof Y.YGOProStocGameMsg) {
      this.handleGameMessage(message.msg)
    } else if (message instanceof Y.YGOProStocErrorMsg) {
      this.emit({ status: 'error', statusText: `Servidor recusou a acao (${message.msg}:${message.code})` })
    } else if (message instanceof Y.YGOProStocChat) {
      this.addLog(message.msg)
    } else if (message instanceof Y.YGOProStocDuelEnd) {
      this.emit({ status: 'finished', statusText: 'Duelo encerrado' })
    }
  }

  uploadDeck() {
    const update = new Y.YGOProCtosUpdateDeck()
    update.deck = new YGOProDeck(BLUE_EYES_DECK)
    this.send(update)
    this.send(new Y.YGOProCtosHsReady())
    this.emit({ statusText: 'Deck Blue-Eyes real enviado; aguardando WindBot...' })
  }

  async launchBot() {
    if (this.botLaunched || !this.state.room) return
    this.botLaunched = true
    const room = this.state.room
    const params = new URLSearchParams({
      name: 'WindBot Blue-Eyes',
      deck: 'BE2025',
      host: 'evolution',
      port: '7911',
      roomid: String(room.roomid),
      password: room.password,
      chat: 'false',
    })
    const response = await fetch(`/windbot/?${params}`)
    if (!response.ok) throw new Error(`WindBot nao iniciou: HTTP ${response.status}`)
    this.addLog('WindBot conectado com o executor Blue-Eyes 2025')
    this.scheduleStart()
  }

  scheduleStart() {
    if (!this.state.isHost || this.state.duelStarted) return
    clearTimeout(this.startTimer)
    this.startTimer = setTimeout(() => {
      if (!this.state.duelStarted) this.send(new Y.YGOProCtosHsStart())
    }, 900)
  }

  handleGameMessage(message) {
    if (!message) return
    this.addLog(message.constructor.name.replace(/^YGOProMsg/, ''))

    if (message instanceof Y.YGOProMsgStart) {
      const localPlayer = message.playerNumber ?? this.state.localPlayer
      this.emit({
        localPlayer,
        lp: [message.startLp0, message.startLp1],
        deckCounts: [message.player0.deckCount, message.player1.deckCount],
        extraCounts: [message.player0.extraCount, message.player1.extraCount],
      })
    } else if (message instanceof Y.YGOProMsgNewTurn) {
      this.emit({ turn: this.state.turn + 1, turnPlayer: message.player, prompt: null })
    } else if (message instanceof Y.YGOProMsgNewPhase) {
      this.emit({ phase: PHASE_NAMES[message.phase] || `FASE ${message.phase}`, prompt: null })
    } else if (message instanceof Y.YGOProMsgUpdateData) {
      this.replaceZone(message.player, message.location, message.cards)
    } else if (message instanceof Y.YGOProMsgUpdateCard) {
      this.updateCard(message.controller, message.location, message.sequence, message.card)
    } else if (message instanceof Y.YGOProMsgMove) {
      this.moveCard(message)
    } else if (message instanceof Y.YGOProMsgDraw) {
      this.applyDraw(message)
    } else if (message instanceof Y.YGOProMsgLpUpdate) {
      this.setLp(message.player, message.lp)
    } else if (message instanceof Y.YGOProMsgDamage) {
      this.setLp(message.player, Math.max(0, this.state.lp[message.player] - message.value))
    } else if (message instanceof Y.YGOProMsgRecover) {
      this.setLp(message.player, this.state.lp[message.player] + message.value)
    } else if (message instanceof Y.YGOProMsgPayLpCost) {
      this.setLp(message.player, Math.max(0, this.state.lp[message.player] - message.cost))
    } else if (message instanceof Y.YGOProMsgWin) {
      this.emit({ winner: message.player, status: 'finished', statusText: 'Partida concluida pelo ocgcore', prompt: null })
    } else if (message instanceof Y.YGOProMsgWaiting) {
      this.emit({ prompt: null, statusText: 'Aguardando a jogada do WindBot...' })
    } else if (message instanceof Y.YGOProMsgSelectChain && message.count === 0 && message.chains.length === 0) {
      this.addLog('Corrente vazia respondida automaticamente')
      this.respondGame(message.prepareResponse(null))
    } else if (typeof message.prepareResponse === 'function') {
      this.emit({ prompt: { type: 'game', message }, statusText: 'Sua decisao' })
    }
  }

  replaceZone(player, location, cards) {
    const zones = { ...this.state.zones }
    const key = zoneKey(player, location)
    const previous = zones[key] || []
    zones[key] = cards.map((card, sequence) => {
      const normalized = normalizeCard(card, player, location, sequence)
      const known = previous[sequence]
      if (normalized && !normalized.code && known?.code && player === this.state.localPlayer && location === LOCATIONS.HAND) {
        normalized.code = known.code
      }
      return normalized
    })
    this.emit({ zones })
  }

  updateCard(player, location, sequence, query) {
    const key = zoneKey(player, location)
    const cards = [...(this.state.zones[key] || [])]
    cards[sequence] = normalizeCard(query, player, location, sequence)
    this.emit({ zones: { ...this.state.zones, [key]: cards } })
  }

  moveCard(message) {
    const zones = { ...this.state.zones }
    const previousKey = zoneKey(message.previous.controller, message.previous.location)
    const currentKey = zoneKey(message.current.controller, message.current.location)
    const previous = [...(zones[previousKey] || [])]
    if (message.previous.location) previous[message.previous.sequence] = null
    zones[previousKey] = previous

    if (message.current.location) {
      const current = [...(zones[currentKey] || [])]
      current[message.current.sequence] = {
        code: message.code,
        controller: message.current.controller,
        location: message.current.location,
        sequence: message.current.sequence,
        position: message.current.position,
      }
      zones[currentKey] = current
    }
    this.emit({ zones })
  }

  applyDraw(message) {
    const key = zoneKey(message.player, LOCATIONS.HAND)
    const cards = [...(this.state.zones[key] || []).filter(Boolean)]
    for (const packedCode of message.cards) {
      cards.push({
        code: packedCode & 0x7fffffff,
        controller: message.player,
        location: LOCATIONS.HAND,
        sequence: cards.length,
        position: packedCode >>> 31 ? 1 : 0,
      })
    }
    const deckCounts = [...this.state.deckCounts]
    deckCounts[message.player] = Math.max(0, deckCounts[message.player] - message.cards.length)
    this.emit({ zones: { ...this.state.zones, [key]: cards }, deckCounts })
  }

  setLp(player, value) {
    if (!Number.isFinite(value)) {
      this.addLog(`LP invalido ignorado para o jogador ${player}`)
      return
    }
    const lp = [...this.state.lp]
    lp[player] = value
    this.emit({ lp })
  }

  respondLobby(type, value) {
    if (type === 'rps') {
      const response = new Y.YGOProCtosHandResult()
      response.res = value
      this.send(response)
    } else if (type === 'turn-order') {
      const response = new Y.YGOProCtosTpResult()
      response.res = value
      this.send(response)
    }
    this.emit({ prompt: null })
  }

  respondGame(payload) {
    const response = new Y.YGOProCtosResponse()
    response.response = payload
    this.send(response)
    this.emit({ prompt: null, statusText: 'Resposta enviada ao ocgcore' })
  }

  surrender() {
    this.send(new Y.YGOProCtosSurrender())
  }

  send(message) {
    this.sendRaw(message.toFullPayload())
  }

  sendRaw(payload) {
    if (this.socket?.readyState === WebSocket.OPEN) this.socket.send(payload)
  }

  cardsAt(player, location) {
    return this.state.zones[zoneKey(player, location)] || []
  }

  disconnect() {
    clearTimeout(this.startTimer)
    this.startTimer = null
    this.socket?.close()
    this.socket = null
    this.buffer = new Uint8Array(0)
    this.botLaunched = false
  }
}

function createJoinGameFrame(roomId, password) {
  // Evolution v2.13.2 reads a 50-byte JOIN_GAME payload. The protocol package
  // emits the older 48-byte form, without the trailing client version.
  const data = new Uint8Array(50)
  const view = new DataView(data.buffer)
  view.setUint16(0, 4962, true)
  view.setUint32(4, roomId, true)
  for (let index = 0; index < Math.min(password.length, 19); index += 1) {
    view.setUint16(8 + index * 2, password.charCodeAt(index), true)
  }
  view.setUint32(46, 4962, true)

  const frame = new Uint8Array(53)
  new DataView(frame.buffer).setUint16(0, 51, true)
  frame[2] = 0x12
  frame.set(data, 3)
  return frame
}
