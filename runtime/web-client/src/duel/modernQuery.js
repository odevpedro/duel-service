const FLAGS = {
  CODE: 0x1,
  POSITION: 0x2,
  ALIAS: 0x4,
  TYPE: 0x8,
  LEVEL: 0x10,
  RANK: 0x20,
  ATTRIBUTE: 0x40,
  RACE: 0x80,
  ATTACK: 0x100,
  DEFENSE: 0x200,
  BASE_ATTACK: 0x400,
  BASE_DEFENSE: 0x800,
  REASON: 0x1000,
  OWNER: 0x40000,
  STATUS: 0x80000,
  IS_PUBLIC: 0x100000,
  LSCALE: 0x200000,
  RSCALE: 0x400000,
  LINK: 0x800000,
  IS_HIDDEN: 0x1000000,
  COVER: 0x2000000,
  END: 0x80000000,
}

export function parseModernUpdateData(frame) {
  const view = dataView(frame)
  const player = frame[4]
  const location = frame[5]
  const size = view.getUint32(6, true)
  return {
    player,
    location,
    cards: parseQueryList(frame.slice(10, 10 + size), player, location),
  }
}

export function parseModernUpdateCard(frame) {
  const player = frame[4]
  const location = frame[5]
  const sequence = frame[6]
  const [card] = parseQueryList(frame.slice(7), player, location, sequence)
  return { player, location, sequence, card }
}

function parseQueryList(payload, player, location, firstSequence = 0) {
  const view = dataView(payload)
  const cards = []
  let offset = 0
  let sequence = firstSequence

  while (offset + 2 <= payload.length) {
    const firstSize = view.getUint16(offset, true)
    if (firstSize === 0) {
      cards.push(null)
      offset += 2
      sequence += 1
      continue
    }

    const card = { flags: 0, controller: player, location, sequence, position: 0 }
    let complete = false
    while (offset + 6 <= payload.length) {
      const fieldSize = view.getUint16(offset, true)
      if (fieldSize < 4 || offset + 2 + fieldSize > payload.length) break
      const flag = view.getUint32(offset + 2, true)
      const valueOffset = offset + 6
      card.flags = (card.flags | flag) >>> 0
      readField(card, flag, view, valueOffset)
      offset += fieldSize + 2
      if (flag === FLAGS.END) {
        complete = true
        break
      }
    }
    cards.push(complete ? card : null)
    if (!complete) break
    sequence += 1
  }
  return cards
}

function readField(card, flag, view, offset) {
  if (flag === FLAGS.CODE) card.code = view.getUint32(offset, true)
  else if (flag === FLAGS.POSITION) card.position = view.getUint32(offset, true)
  else if (flag === FLAGS.ALIAS) card.alias = view.getUint32(offset, true)
  else if (flag === FLAGS.TYPE) card.type = view.getUint32(offset, true)
  else if (flag === FLAGS.LEVEL) card.level = view.getUint32(offset, true)
  else if (flag === FLAGS.RANK) card.rank = view.getUint32(offset, true)
  else if (flag === FLAGS.ATTRIBUTE) card.attribute = view.getUint32(offset, true)
  else if (flag === FLAGS.RACE) card.race = Number(view.getBigUint64(offset, true))
  else if (flag === FLAGS.ATTACK) card.attack = view.getInt32(offset, true)
  else if (flag === FLAGS.DEFENSE) card.defense = view.getInt32(offset, true)
  else if (flag === FLAGS.BASE_ATTACK) card.baseAttack = view.getInt32(offset, true)
  else if (flag === FLAGS.BASE_DEFENSE) card.baseDefense = view.getInt32(offset, true)
  else if (flag === FLAGS.REASON) card.reason = view.getUint32(offset, true)
  else if (flag === FLAGS.OWNER) card.owner = view.getUint8(offset)
  else if (flag === FLAGS.STATUS) card.status = view.getUint32(offset, true)
  else if (flag === FLAGS.IS_PUBLIC) card.isPublic = view.getUint8(offset) !== 0
  else if (flag === FLAGS.LSCALE) card.leftScale = view.getUint32(offset, true)
  else if (flag === FLAGS.RSCALE) card.rightScale = view.getUint32(offset, true)
  else if (flag === FLAGS.LINK) {
    card.link = view.getUint32(offset, true)
    card.linkMarker = view.getUint32(offset + 4, true)
  } else if (flag === FLAGS.IS_HIDDEN) card.isHidden = view.getUint8(offset) !== 0
  else if (flag === FLAGS.COVER) card.cover = view.getUint32(offset, true)
}

function dataView(bytes) {
  return new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength)
}
