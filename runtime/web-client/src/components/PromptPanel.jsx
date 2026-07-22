import { useEffect, useMemo, useState } from 'react'
import * as Y from 'ygopro-msg-encode'
import CardTile from './CardTile'
import { getCard, resolveDescription } from '../duel/cardDatabase'

function PromptCard({ card, index, selected, onClick, verb }) {
  return (
    <div className="prompt-card">
      <CardTile card={card} compact selected={selected} onClick={onClick} badge={verb} />
      <span className="prompt-card-index">#{index + 1}</span>
    </div>
  )
}

function CardAction({ card, index, verb, onClick, description }) {
  const [name, setName] = useState(card.code ? `Carta ${card.code}` : 'Carta')
  const [effect, setEffect] = useState('')
  useEffect(() => {
    let active = true
    getCard(card.code).then(value => active && setName(value.name))
    if (description) resolveDescription(description).then(value => active && setEffect(value))
    return () => { active = false }
  }, [card.code, description])
  return (
    <button type="button" className="choice-row" onClick={onClick}>
      <span>{verb}</span>
      <strong>{name}</strong>
      {effect && <small>{effect}</small>}
      <small>#{index + 1}</small>
    </button>
  )
}

function GamePrompt({ message, onRespond }) {
  const [selected, setSelected] = useState([])
  const [sortOrder, setSortOrder] = useState([])
  const [placeSelection, setPlaceSelection] = useState([])
  const [counterValues, setCounterValues] = useState({})
  const [bitSelection, setBitSelection] = useState(0)
  const [announcedCard, setAnnouncedCard] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    setSelected([])
    setSortOrder([])
    setPlaceSelection([])
    setCounterValues({})
    setBitSelection(0)
    setAnnouncedCard('')
    setError('')
  }, [message])

  const send = callback => {
    try {
      onRespond(callback())
    } catch (reason) {
      setError(reason.message || String(reason))
    }
  }

  const toggle = index => setSelected(current => current.includes(index)
    ? current.filter(value => value !== index)
    : [...current, index])

  if (message instanceof Y.YGOProMsgSelectIdleCmd) {
    const groups = [
      [Y.IdleCmdType.SUMMON, 'Invocar', message.summonableCards],
      [Y.IdleCmdType.SPSUMMON, 'Invocacao-Especial', message.spSummonableCards],
      [Y.IdleCmdType.REPOS, 'Mudar posicao', message.reposableCards],
      [Y.IdleCmdType.MSET, 'Baixar monstro', message.msetableCards],
      [Y.IdleCmdType.SSET, 'Baixar magia/armadilha', message.ssetableCards],
      [Y.IdleCmdType.ACTIVATE, 'Ativar efeito', message.activatableCards],
    ]
    return (
      <Prompt title="Escolha uma acao" error={error}>
        {groups.flatMap(([type, label, cards]) => cards.map((card, index) => (
          <CardAction key={`${type}-${index}`} card={card} index={index} verb={label}
            description={card.desc} onClick={() => send(() => message.prepareResponse(type, Y.IndexResponse(index)))} />
        )))}
        <div className="prompt-buttons">
          {!!message.canBp && <button onClick={() => send(() => message.prepareResponse(Y.IdleCmdType.TO_BP))}>Fase de Batalha</button>}
          {!!message.canEp && <button onClick={() => send(() => message.prepareResponse(Y.IdleCmdType.TO_EP))}>Encerrar turno</button>}
          {!!message.canShuffle && <button onClick={() => send(() => message.prepareResponse(Y.IdleCmdType.SHUFFLE))}>Embaralhar mao</button>}
        </div>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSelectBattleCmd) {
    return (
      <Prompt title="Fase de Batalha" error={error}>
        {message.activatableCards.map((card, index) => (
          <CardAction key={`activate-${index}`} card={card} index={index} verb="Ativar"
            description={card.desc} onClick={() => send(() => message.prepareResponse(Y.BattleCmdType.ACTIVATE, Y.IndexResponse(index)))} />
        ))}
        {message.attackableCards.map((card, index) => (
          <CardAction key={`attack-${index}`} card={card} index={index} verb={card.directAttack ? 'Ataque direto' : 'Atacar'}
            onClick={() => send(() => message.prepareResponse(Y.BattleCmdType.ATTACK, Y.IndexResponse(index)))} />
        ))}
        <div className="prompt-buttons">
          {!!message.canM2 && <button onClick={() => send(() => message.prepareResponse(Y.BattleCmdType.TO_M2))}>Main Phase 2</button>}
          {!!message.canEp && <button onClick={() => send(() => message.prepareResponse(Y.BattleCmdType.TO_EP))}>Encerrar turno</button>}
        </div>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSelectEffectYn || message instanceof Y.YGOProMsgSelectYesNo) {
    return (
      <Prompt title="Ativar este efeito?" error={error}>
        <div className="prompt-buttons">
          <button className="primary" onClick={() => send(() => message.prepareResponse(true))}>Sim</button>
          <button onClick={() => send(() => message.prepareResponse(false))}>Nao</button>
        </div>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSelectChain) {
    const forced = message.chains.some(chain => chain.forced)
    return (
      <Prompt title="Responder na corrente?" error={error}>
        {message.chains.map((card, index) => (
          <CardAction key={index} card={card} index={index} verb="Encadear"
            description={card.desc} onClick={() => send(() => message.prepareResponse(Y.IndexResponse(index)))} />
        ))}
        {!forced && <button onClick={() => send(() => message.prepareResponse(null))}>Nao responder</button>}
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSelectOption) {
    return <OptionPrompt message={message} title="Escolha uma opcao" onSelect={index => send(() => message.prepareResponse(Y.IndexResponse(index)))} error={error} />
  }

  if (message instanceof Y.YGOProMsgAnnounceNumber) {
    return (
      <Prompt title="Declare um numero" error={error}>
        <div className="prompt-buttons">
          {message.numbers.map((number, index) => <button key={index} onClick={() => send(() => message.prepareResponse(Y.IndexResponse(index)))}>{number}</button>)}
        </div>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSelectPosition) {
    const positions = [[1, 'Ataque aberto'], [2, 'Defesa aberta'], [4, 'Ataque baixado'], [8, 'Defesa baixada']]
    return (
      <Prompt title="Escolha a posicao" error={error}>
        <div className="prompt-buttons">
          {positions.filter(([value]) => message.positions & value).map(([value, label]) => (
            <button key={value} onClick={() => send(() => message.prepareResponse(value))}>{label}</button>
          ))}
        </div>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSelectPlace || message instanceof Y.YGOProMsgSelectDisField) {
    const places = message.getSelectablePlaces()
    const togglePlace = index => setPlaceSelection(current => current.includes(index)
      ? current.filter(value => value !== index)
      : current.length < message.count ? [...current, index] : current)
    return (
      <Prompt title={`Escolha ${message.count} zona(s)`} error={error}>
        <div className="place-grid">
          {places.map((place, index) => (
            <button key={`${place.player}-${place.location}-${place.sequence}`}
              className={placeSelection.includes(index) ? 'is-selected' : ''} onClick={() => togglePlace(index)}>
              {place.player === message.player ? 'Seu' : 'Oponente'} {place.location === 4 ? 'Monstro' : 'Magia'} {place.sequence + 1}
            </button>
          ))}
        </div>
        <button className="primary" disabled={placeSelection.length !== message.count}
          onClick={() => send(() => message.prepareResponse(placeSelection.map(index => places[index])))}>Confirmar zonas</button>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSelectUnselectCard) {
    return (
      <Prompt title="Selecione ou remova uma carta" error={error}>
        <div className="prompt-card-grid">
          {message.selectableCards.map((card, index) => <PromptCard key={`s-${index}`} card={card} index={index} verb="Selecionar"
            onClick={() => send(() => message.prepareResponse(Y.IndexResponse(index)))} />)}
          {message.unselectableCards.map((card, index) => <PromptCard key={`u-${index}`} card={card} index={index} verb="Remover"
            onClick={() => send(() => message.prepareResponse(Y.IndexResponse(message.selectableCount + index)))} />)}
        </div>
        {!!message.finishable && <button className="primary" onClick={() => send(() => message.prepareResponse(null))}>Concluir</button>}
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSelectCard || message instanceof Y.YGOProMsgSelectTribute || message instanceof Y.YGOProMsgSelectSum) {
    const cards = message.cards || []
    const minimum = message instanceof Y.YGOProMsgSelectSum ? 1 : message.min
    const maximum = message.max || cards.length
    const fixed = message.mustSelectCards || []
    return (
      <Prompt title={`Selecione cartas (${minimum}-${maximum})`} error={error}>
        {fixed.length > 0 && <p className="prompt-note">{fixed.length} carta(s) obrigatoria(s) ja incluida(s).</p>}
        <div className="prompt-card-grid">
          {cards.map((card, index) => <PromptCard key={index} card={card} index={index} selected={selected.includes(index)} onClick={() => toggle(index)} />)}
        </div>
        <div className="prompt-buttons">
          <button className="primary" disabled={selected.length < minimum || selected.length > maximum}
            onClick={() => send(() => message.prepareResponse(selected.map(Y.IndexResponse)))}>Confirmar</button>
          {!!message.cancelable && <button onClick={() => send(() => message.prepareResponse(null))}>Cancelar</button>}
        </div>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSortCard) {
    const addSort = index => setSortOrder(current => current.includes(index) ? current : [...current, index])
    return (
      <Prompt title="Defina a ordem das cartas" error={error}>
        <div className="prompt-card-grid">
          {message.cards.map((card, index) => <PromptCard key={index} card={card} index={index}
            selected={sortOrder.includes(index)} badge={sortOrder.includes(index) ? String(sortOrder.indexOf(index) + 1) : ''} onClick={() => addSort(index)} />)}
        </div>
        <div className="prompt-buttons">
          <button onClick={() => setSortOrder([])}>Limpar</button>
          <button className="primary" disabled={sortOrder.length !== message.cards.length}
            onClick={() => send(() => message.prepareResponse(sortOrder.map(Y.IndexResponse)))}>Confirmar ordem</button>
        </div>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgSelectCounter) {
    const total = Object.values(counterValues).reduce((sum, count) => sum + count, 0)
    return (
      <Prompt title={`Distribua ${message.counterCount} contador(es)`} error={error}>
        {message.cards.map((card, index) => (
          <label className="counter-row" key={index}>
            <span>{card.code}</span>
            <input type="number" min="0" max={card.counterCount} value={counterValues[index] || 0}
              onChange={event => setCounterValues(current => ({ ...current, [index]: Number(event.target.value) }))} />
            <small>max. {card.counterCount}</small>
          </label>
        ))}
        <button className="primary" disabled={total !== message.counterCount} onClick={() => send(() => message.prepareResponse(
          Object.entries(counterValues).filter(([, count]) => count > 0).map(([index, count]) => ({ card: Y.IndexResponse(Number(index)), count })),
        ))}>Confirmar ({total}/{message.counterCount})</button>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgAnnounceRace || message instanceof Y.YGOProMsgAnnounceAttrib) {
    const isRace = message instanceof Y.YGOProMsgAnnounceRace
    const available = isRace ? message.availableRaces : message.availableAttributes
    const options = isRace ? RACES : ATTRIBUTES
    const required = message.count
    const selectedCount = options.filter(([value]) => bitSelection & value).length
    return (
      <Prompt title={`Escolha ${required} ${isRace ? 'tipo(s)' : 'atributo(s)'}`} error={error}>
        <div className="prompt-buttons">
          {options.filter(([value]) => available & value).map(([value, label]) => (
            <button key={value} className={bitSelection & value ? 'is-selected' : ''}
              onClick={() => setBitSelection(current => current ^ value)}>{label}</button>
          ))}
        </div>
        <button className="primary" disabled={selectedCount !== required} onClick={() => send(() => message.prepareResponse(bitSelection))}>Confirmar</button>
      </Prompt>
    )
  }

  if (message instanceof Y.YGOProMsgAnnounceCard) {
    return (
      <Prompt title="Declare o codigo de uma carta" error={error}>
        <label className="announce-card"><span>Codigo da carta</span><input inputMode="numeric" value={announcedCard} onChange={event => setAnnouncedCard(event.target.value)} /></label>
        <button className="primary" disabled={!/^\d+$/.test(announcedCard)} onClick={() => send(() => message.prepareResponse(Number(announcedCard)))}>Confirmar</button>
      </Prompt>
    )
  }

  return (
    <Prompt title="Decisao do ocgcore" error={error}>
      <p className="prompt-note">{message.constructor.name}</p>
      {message.defaultResponse?.() && <button onClick={() => send(() => message.defaultResponse())}>Resposta padrao segura</button>}
    </Prompt>
  )
}

function OptionPrompt({ message, title, onSelect, error }) {
  const [labels, setLabels] = useState([])
  useEffect(() => {
    let active = true
    Promise.all(message.options.map(resolveDescription)).then(values => active && setLabels(values))
    return () => { active = false }
  }, [message])
  return (
    <Prompt title={title} error={error}>
      {message.options.map((option, index) => <button className="choice-row" key={index} onClick={() => onSelect(index)}>{labels[index] || `Opcao ${option}`}</button>)}
    </Prompt>
  )
}

function Prompt({ title, error, children }) {
  return (
    <section className="prompt-panel">
      <div className="panel-heading"><span>DECISAO</span><h2>{title}</h2></div>
      <div className="prompt-content">{children}</div>
      {error && <p className="prompt-error">{error}</p>}
    </section>
  )
}

export default function PromptPanel({ prompt, onLobby, onGame }) {
  if (!prompt) return (
    <section className="prompt-panel prompt-panel--waiting">
      <div className="waiting-pulse" />
      <p>Aguardando uma decisao do motor</p>
    </section>
  )
  if (prompt.type === 'rps') return (
    <Prompt title="Pedra, papel ou tesoura">
      <div className="prompt-buttons"><button onClick={() => onLobby('rps', 1)}>Pedra</button><button onClick={() => onLobby('rps', 2)}>Tesoura</button><button onClick={() => onLobby('rps', 3)}>Papel</button></div>
    </Prompt>
  )
  if (prompt.type === 'turn-order') return (
    <Prompt title="Quem comeca?">
      <div className="prompt-buttons"><button className="primary" onClick={() => onLobby('turn-order', 1)}>Eu comeco</button><button onClick={() => onLobby('turn-order', 0)}>WindBot comeca</button></div>
    </Prompt>
  )
  return <GamePrompt message={prompt.message} onRespond={onGame} />
}

const ATTRIBUTES = [[1, 'Terra'], [2, 'Agua'], [4, 'Fogo'], [8, 'Vento'], [16, 'Luz'], [32, 'Trevas'], [64, 'Divino']]
const RACES = [[1, 'Guerreiro'], [2, 'Mago'], [4, 'Fada'], [8, 'Demonio'], [16, 'Zumbi'], [32, 'Maquina'], [64, 'Aqua'], [128, 'Pyro'], [256, 'Rocha'], [512, 'Besta Alada'], [1024, 'Planta'], [2048, 'Inseto'], [4096, 'Trovao'], [8192, 'Dragao'], [16384, 'Besta'], [32768, 'Besta-Guerreira'], [65536, 'Dinossauro'], [131072, 'Peixe'], [262144, 'Serpente Marinha'], [524288, 'Reptil'], [1048576, 'Psiquico'], [2097152, 'Besta Divina'], [4194304, 'Deus Criador'], [8388608, 'Wyrm'], [16777216, 'Cyberse'], [33554432, 'Ilusao']]
