export default function SelectionCard({ selected, icon, label, meta, onClick }) {
  return (
    <button
      type="button"
      className={`selection-card${selected ? ' selected' : ''}`}
      onClick={onClick}
      aria-pressed={selected}
    >
      {icon && <span className="icon" aria-hidden="true">{icon}</span>}
      <span>
        <div className="label">{label}</div>
        {meta && <div className="meta">{meta}</div>}
      </span>
    </button>
  )
}
