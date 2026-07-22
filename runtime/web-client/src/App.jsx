import { useEffect, useMemo, useRef, useState } from 'react'
import CardTile from './components/CardTile'
import PromptPanel from './components/PromptPanel'
import { BLUE_EYES_DECK, LOCATIONS, LocalDuelClient } from './duel/LocalDuelClient'
import { getCard, preloadCards } from './duel/cardDatabase'

const INITIAL_STATE = {
  status: 'idle', statusText: 'Runtime local pronto', players: ['', ''], localPlayer: 0,
  lp: [8000, 8000], turn: 0, turnPlayer: 0, phase: '', zones: {}, deckCounts: [40, 40],
  extraCounts: [15, 15], prompt: null, winner: null, log: [], duelStarted: false,
}

function cardsAt(state, player, location) {
  return state.zones[`${player}:${location}`] || []
}

function FieldRow({ cards, label, count }) {
  const slots = Array.from({ length: count }, (_, index) => cards[index] || null)
  return (
    <div className="field-row-wrap">
      <span className="field-row-label">{label}</span>
      <div className="field-row">
        {slots.map((card, index) => <CardTile key={index} card={card} compact />)}
      </div>
    </div>
  )
}

function PlayerHeader({ name, lp, deck, extra, active, opponent }) {
  return (
    <header className={`player-header ${active ? 'is-active' : ''}`}>
      <div className="player-identity"><span>{opponent ? 'IA LOCAL' : 'VOCE'}</span><strong>{name || (opponent ? 'WindBot' : 'Duelista')}</strong></div>
      <div className="player-stat"><small>LP</small><strong>{lp}</strong></div>
      <div className="player-stat"><small>DECK</small><strong>{deck}</strong></div>
      <div className="player-stat"><small>EXTRA</small><strong>{extra}</strong></div>
    </header>
  )
}

function DuelBoard({ state, onInspect }) {
  const self = state.localPlayer
  const opponent = self === 0 ? 1 : 0
  const opponentHand = cardsAt(state, opponent, LOCATIONS.HAND).filter(Boolean)
  const hand = cardsAt(state, self, LOCATIONS.HAND).filter(Boolean)

  return (
    <main className="duel-board">
      <PlayerHeader opponent name={state.players[opponent]} lp={state.lp[opponent]} deck={state.deckCounts[opponent]}
        extra={state.extraCounts[opponent]} active={state.turnPlayer === opponent} />
      <div className="opponent-hand" aria-label={`${opponentHand.length} cartas na mao do oponente`}>
        {opponentHand.map((card, index) => <CardTile key={index} card={{ ...card, code: 0 }} compact />)}
      </div>

      <div className="field-surface">
        <FieldRow label="MAGIAS IA" cards={cardsAt(state, opponent, LOCATIONS.SZONE)} count={8} />
        <FieldRow label="MONSTROS IA" cards={cardsAt(state, opponent, LOCATIONS.MZONE)} count={7} />
        <div className="field-divider">
          <span>TURNO {state.turn || '-'}</span>
          <strong>{state.phase || 'PREPARANDO'}</strong>
          <span>{state.turnPlayer === self ? 'SUA VEZ' : 'VEZ DA IA'}</span>
        </div>
        <FieldRow label="SEUS MONSTROS" cards={cardsAt(state, self, LOCATIONS.MZONE)} count={7} />
        <FieldRow label="SUAS MAGIAS" cards={cardsAt(state, self, LOCATIONS.SZONE)} count={8} />
      </div>

      <div className="hand-section">
        <span className="hand-label">SUA MAO <strong>{hand.length}</strong></span>
        <div className="hand-row">
          {hand.map((card, index) => <CardTile key={`${card.code}-${index}`} card={card} onClick={() => onInspect(card)} />)}
        </div>
      </div>
      <PlayerHeader name={state.players[self]} lp={state.lp[self]} deck={state.deckCounts[self]}
        extra={state.extraCounts[self]} active={state.turnPlayer === self} />
    </main>
  )
}

function CardInspector({ card, onClose }) {
  const [metadata, setMetadata] = useState(null)
  useEffect(() => {
    let active = true
    if (card?.code) getCard(card.code).then(value => active && setMetadata(value))
    return () => { active = false }
  }, [card])
  if (!card) return null
  return (
    <div className="inspector-backdrop" onClick={onClose}>
      <article className="card-inspector" onClick={event => event.stopPropagation()}>
        <button className="icon-close" onClick={onClose} aria-label="Fechar">x</button>
        <img src={`/local-assets/cards/${card.code}.jpg`} alt={metadata?.name || ''} />
        <div><span>CARTA {card.code}</span><h2>{metadata?.name || `Carta ${card.code}`}</h2>
          {metadata?.attack >= 0 && <p className="card-stats">ATK {metadata.attack} / DEF {metadata.defense}</p>}
          <p>{metadata?.description}</p></div>
      </article>
    </div>
  )
}

export default function App() {
  const [state, setState] = useState(INITIAL_STATE)
  const [playerName, setPlayerName] = useState('Duelista')
  const [error, setError] = useState('')
  const [inspectedCard, setInspectedCard] = useState(null)
  const clientRef = useRef(null)

  if (!clientRef.current) clientRef.current = new LocalDuelClient(setState)
  const client = clientRef.current

  useEffect(() => {
    preloadCards([...BLUE_EYES_DECK.main, ...BLUE_EYES_DECK.extra]).catch(() => {})
    return () => client.disconnect()
  }, [client])

  const canStart = useMemo(() => !['creating', 'connecting', 'lobby', 'dueling'].includes(state.status), [state.status])

  const start = async () => {
    setError('')
    try {
      await client.start(playerName.trim() || 'Duelista')
    } catch (reason) {
      setError(reason.message || String(reason))
      client.emit({ status: 'error', statusText: 'Falha ao iniciar o duelo' })
    }
  }

  if (!state.duelStarted && state.status !== 'finished') {
    return (
      <div className="setup-screen">
        <section className="setup-panel">
          <div className="brand-lockup"><span>OCGCORE / WIND BOT</span><h1>Duelo local</h1></div>
          <div className="runtime-line"><span className={`status-dot status-dot--${state.status}`} /><strong>{state.statusText}</strong></div>
          <label className="name-field"><span>Nome do duelista</span><input maxLength="20" value={playerName} onChange={event => setPlayerName(event.target.value)} /></label>
          <div className="deck-summary">
            <div><small>DECK</small><strong>Blue-Eyes 2025</strong></div><span>40 MAIN</span><span>15 EXTRA</span>
          </div>
          <button className="start-button" disabled={!canStart} onClick={start}>{canStart ? 'Iniciar contra WindBot' : 'Preparando duelo...'}</button>
          {error && <p className="setup-error">{error}</p>}
        </section>
      </div>
    )
  }

  const selfWon = state.winner === state.localPlayer
  return (
    <div className="app-shell">
      <div className="top-strip">
        <div><span className={`status-dot status-dot--${state.status}`} /><strong>{state.statusText}</strong></div>
        <button className="danger-link" onClick={() => client.surrender()}>Render-se</button>
      </div>
      <div className="game-layout">
        <DuelBoard state={state} onInspect={setInspectedCard} />
        <aside className="decision-column">
          {state.winner !== null ? (
            <section className="result-panel"><span>RESULTADO</span><h2>{selfWon ? 'Vitoria' : 'Derrota'}</h2><button onClick={start}>Jogar novamente</button></section>
          ) : <PromptPanel prompt={state.prompt} onLobby={(type, value) => client.respondLobby(type, value)} onGame={payload => client.respondGame(payload)} />}
          <details className="protocol-log"><summary>Eventos do motor</summary><div>{state.log.map((line, index) => <code key={index}>{line}</code>)}</div></details>
        </aside>
      </div>
      <CardInspector card={inspectedCard} onClose={() => setInspectedCard(null)} />
    </div>
  )
}
