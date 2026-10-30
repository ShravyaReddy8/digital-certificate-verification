export function formatDate(value, fallback = '—') {
  if (!value) return fallback;
  const raw = String(value);
  const dateOnly = /^\d{4}-\d{2}-\d{2}$/.test(raw);
  const date = dateOnly ? new Date(`${raw}T00:00:00Z`) : new Date(raw);
  if (Number.isNaN(date.getTime())) return fallback;
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    ...(dateOnly ? { timeZone: 'UTC' } : {}),
  }).format(date);
}

export function formatDateTime(value, fallback = '—') {
  if (!value) return fallback;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return fallback;
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatBytes(bytes) {
  if (!bytes && bytes !== 0) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function todayIso() {
  return new Date().toISOString().slice(0, 10);
}
