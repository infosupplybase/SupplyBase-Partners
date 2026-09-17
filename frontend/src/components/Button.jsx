export default function Button({ variant = 'primary', block, children, className = '', ...props }) {
  const cls = `btn btn-${variant} ${block ? 'btn-block' : ''} ${className}`.trim()
  return (
    <button className={cls} {...props}>
      {children}
    </button>
  )
}
