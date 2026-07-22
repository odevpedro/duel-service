import * as Y from 'ygopro-msg-encode'

const MSG = {
  START: 4,
  SELECT_BATTLECMD: 10,
  SELECT_IDLECMD: 11,
  SELECT_EFFECTYN: 12,
  SELECT_YESNO: 13,
  SELECT_OPTION: 14,
  SELECT_CARD: 15,
  SELECT_CHAIN: 16,
  SELECT_TRIBUTE: 20,
  SELECT_COUNTER: 22,
  SELECT_SUM: 23,
  SELECT_UNSELECT_CARD: 26,
}

export function parseModernGameMessage(frame) {
  if (frame[2] !== 0x01) return null

  const id = frame[3]
  const reader = new Reader(frame.subarray(4))
  switch (id) {
    case MSG.START: return parseStart(reader)
    case MSG.SELECT_BATTLECMD: return parseBattleCommand(reader)
    case MSG.SELECT_IDLECMD: return parseIdleCommand(reader)
    case MSG.SELECT_EFFECTYN: return parseEffectYesNo(reader)
    case MSG.SELECT_YESNO: return parseYesNo(reader)
    case MSG.SELECT_OPTION: return parseOption(reader)
    case MSG.SELECT_CARD: return parseSelectCard(reader)
    case MSG.SELECT_CHAIN: return parseChain(reader)
    case MSG.SELECT_TRIBUTE: return parseTribute(reader)
    case MSG.SELECT_COUNTER: return parseCounter(reader)
    case MSG.SELECT_SUM: return parseSum(reader)
    case MSG.SELECT_UNSELECT_CARD: return parseUnselectCard(reader)
    default: return null
  }
}

function parseStart(reader) {
  const message = new Y.YGOProMsgStart()
  message.playerNumber = reader.u8() & 0x0f
  message.startLp0 = reader.u32()
  message.startLp1 = reader.u32()
  message.player0 = { deckCount: reader.u16(), extraCount: reader.u16() }
  message.player1 = { deckCount: reader.u16(), extraCount: reader.u16() }
  return message
}

function parseIdleCommand(reader) {
  const message = new Y.YGOProMsgSelectIdleCmd()
  message.player = reader.u8()
  message.summonableCards = readArray(reader, () => simpleCard(reader, true))
  message.spSummonableCards = readArray(reader, () => simpleCard(reader, true))
  message.reposableCards = readArray(reader, () => simpleCard(reader, false))
  message.msetableCards = readArray(reader, () => simpleCard(reader, true))
  message.ssetableCards = readArray(reader, () => simpleCard(reader, true))
  message.activatableCards = readArray(reader, () => activatableCard(reader))
  setCounts(message)
  message.canBp = reader.u8()
  message.canEp = reader.u8()
  message.canShuffle = reader.u8()
  return message
}

function parseBattleCommand(reader) {
  const message = new Y.YGOProMsgSelectBattleCmd()
  message.player = reader.u8()
  message.activatableCards = readArray(reader, () => activatableCard(reader))
  message.attackableCards = readArray(reader, () => ({
    ...simpleCard(reader, false),
    directAttack: reader.u8(),
  }))
  message.activatableCount = message.activatableCards.length
  message.attackableCount = message.attackableCards.length
  message.canM2 = reader.u8()
  message.canEp = reader.u8()
  return message
}

function parseEffectYesNo(reader) {
  const message = new Y.YGOProMsgSelectEffectYn()
  message.player = reader.u8()
  Object.assign(message, locationCard(reader))
  message.desc = reader.u64()
  return message
}

function parseYesNo(reader) {
  const message = new Y.YGOProMsgSelectYesNo()
  message.player = reader.u8()
  message.desc = reader.u64()
  return message
}

function parseOption(reader) {
  const message = new Y.YGOProMsgSelectOption()
  message.player = reader.u8()
  const count = reader.u8()
  message.options = Array.from({ length: count }, () => reader.u64())
  message.count = count
  return message
}

function parseSelectCard(reader) {
  const message = new Y.YGOProMsgSelectCard()
  message.player = reader.u8()
  message.cancelable = reader.u8()
  message.min = reader.u32()
  message.max = reader.u32()
  message.cards = readArray(reader, () => locationCard(reader))
  message.count = message.cards.length
  useModernSelectionResponse(message)
  return message
}

function parseChain(reader) {
  const message = new Y.YGOProMsgSelectChain()
  message.player = reader.u8()
  message.specialCount = reader.u8()
  message.forced = reader.u8()
  message.hint0 = reader.u32()
  message.hint1 = reader.u32()
  message.chains = readArray(reader, () => ({
    ...locationCard(reader),
    desc: reader.u64(),
    flag: reader.u8(),
    forced: message.forced,
  }))
  message.count = message.chains.length
  return message
}

function parseTribute(reader) {
  const message = new Y.YGOProMsgSelectTribute()
  message.player = reader.u8()
  message.cancelable = reader.u8()
  message.min = reader.u32()
  message.max = reader.u32()
  message.cards = readArray(reader, () => ({
    ...simpleCard(reader, true),
    releaseParam: reader.u8(),
  }))
  message.count = message.cards.length
  useModernSelectionResponse(message)
  return message
}

function parseCounter(reader) {
  const message = new Y.YGOProMsgSelectCounter()
  message.player = reader.u8()
  message.counterType = reader.u16()
  message.counterCount = reader.u16()
  message.cards = readArray(reader, () => ({
    ...simpleCard(reader, false),
    counterCount: reader.u16(),
  }))
  message.count = message.cards.length
  return message
}

function parseSum(reader) {
  const message = new Y.YGOProMsgSelectSum()
  message.player = reader.u8()
  message.mode = reader.u8()
  message.sumValue = reader.u32()
  message.min = reader.u32()
  message.max = reader.u32()
  message.mustSelectCards = readArray(reader, () => sumCard(reader))
  message.mustSelectCount = message.mustSelectCards.length
  message.cards = readArray(reader, () => sumCard(reader))
  message.count = message.cards.length
  useModernSelectionResponse(message)
  return message
}

function parseUnselectCard(reader) {
  const message = new Y.YGOProMsgSelectUnselectCard()
  message.player = reader.u8()
  message.finishable = reader.u8()
  message.cancelable = reader.u8()
  message.min = reader.u32()
  message.max = reader.u32()
  message.selectableCards = readArray(reader, () => locationCard(reader))
  message.selectableCount = message.selectableCards.length
  message.unselectableCards = readArray(reader, () => locationCard(reader))
  message.unselectableCount = message.unselectableCards.length
  message.prepareResponse = option => {
    if (option == null) return int32(-1)
    const index = responseIndex(option)
    const response = new Uint8Array(8)
    const view = new DataView(response.buffer)
    view.setUint32(0, 1, true)
    view.setUint32(4, index, true)
    return response
  }
  return message
}

function simpleCard(reader, wideSequence) {
  return {
    code: reader.u32(),
    controller: reader.u8(),
    location: reader.u8(),
    sequence: wideSequence ? reader.u32() : reader.u8(),
  }
}

function locationCard(reader) {
  const card = simpleCard(reader, true)
  card.position = reader.u32()
  card.subsequence = card.position
  return card
}

function activatableCard(reader) {
  return {
    ...simpleCard(reader, true),
    desc: reader.u64(),
    flag: reader.u8(),
  }
}

function sumCard(reader) {
  return { ...locationCard(reader), opParam: reader.u32() }
}

function readArray(reader, readItem) {
  const count = reader.u32()
  return Array.from({ length: count }, readItem)
}

function setCounts(message) {
  message.summonableCount = message.summonableCards.length
  message.spSummonableCount = message.spSummonableCards.length
  message.reposableCount = message.reposableCards.length
  message.msetableCount = message.msetableCards.length
  message.ssetableCount = message.ssetableCards.length
  message.activatableCount = message.activatableCards.length
}

function useModernSelectionResponse(message) {
  message.prepareResponse = options => {
    if (options == null) return int32(-1)
    const indices = options.map(responseIndex)
    if (indices.length < message.min || indices.length > message.max) {
      throw new TypeError(`Selecione entre ${message.min} e ${message.max} cartas`)
    }
    if (indices.some(index => index < 0 || index >= message.count)) {
      throw new TypeError('Indice de carta fora do intervalo')
    }
    return selectedIndices(indices)
  }
}

function responseIndex(option) {
  if (typeof option === 'number') return option
  if (option && Number.isInteger(option.index)) return option.index
  throw new TypeError('Resposta de carta invalida')
}

function selectedIndices(indices) {
  const response = new Uint8Array(8 + indices.length)
  const view = new DataView(response.buffer)
  view.setInt32(0, 2, true)
  view.setUint32(4, indices.length, true)
  indices.forEach((index, offset) => { response[8 + offset] = index })
  return response
}

function int32(value) {
  const response = new Uint8Array(4)
  new DataView(response.buffer).setInt32(0, value, true)
  return response
}

class Reader {
  constructor(bytes) {
    this.bytes = bytes
    this.view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength)
    this.offset = 0
  }

  require(size) {
    if (this.offset + size > this.bytes.length) {
      throw new RangeError(`Prompt moderno truncado em ${this.offset}: faltam ${size} bytes`)
    }
  }

  u8() {
    this.require(1)
    return this.view.getUint8(this.offset++)
  }

  u16() {
    this.require(2)
    const value = this.view.getUint16(this.offset, true)
    this.offset += 2
    return value
  }

  u32() {
    this.require(4)
    const value = this.view.getUint32(this.offset, true)
    this.offset += 4
    return value
  }

  u64() {
    this.require(8)
    const value = Number(this.view.getBigUint64(this.offset, true))
    this.offset += 8
    return value
  }
}
