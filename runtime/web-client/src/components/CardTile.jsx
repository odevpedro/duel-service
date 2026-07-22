import { useEffect, useState } from 'react'
import { getCard } from '../duel/cardDatabase'

export default function CardTile({ card, compact = false, selected = false, onClick, badge }) {
  const [metadata, setMetadata] = useState(null)
  const code = Number(card?.code) || 0

  useEffect(() => {
    let active = true
    getCard(code).then(value => active && setMetadata(value))
    return () => { active = false }
  }, [code])

  if (!card) return <div className={`card-tile card-tile--empty ${compact ? 'card-tile--compact' : ''}`} />
  const hidden = !code

  return (
    <button
      type="button"
      className={`card-tile ${compact ? 'card-tile--compact' : ''} ${selected ? 'is-selected' : ''} ${hidden ? 'is-hidden' : ''}`}
      onClick={onClick}
      disabled={!onClick}
      title={metadata?.name || (hidden ? 'Carta oculta' : `Carta ${code}`)}
    >
      {hidden ? (
        <span className="card-back-mark">DUEL</span>
      ) : (
        <img src={`/local-assets/cards/${code}.jpg`} alt={metadata?.name || `Carta ${code}`} />
      )}
      <span className="card-name">{metadata?.name || (hidden ? 'Carta oculta' : code)}</span>
      {badge && <span className="card-badge">{badge}</span>}
    </button>
  )
}
