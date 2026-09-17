export default function TextField({ label, error, id, as = 'input', ...props }) {
  const Tag = as
  return (
    <div className="field">
      {label && <label htmlFor={id}>{label}</label>}
      <Tag id={id} {...props} />
      {error && <span className="error">{error}</span>}
    </div>
  )
}
