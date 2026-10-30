import axios from 'axios';

export const TOKEN_KEY = 'certverify_token';
export const USER_KEY = 'certverify_user';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function getErrorMessage(error) {
  if (error?.code === 'ECONNABORTED') {
    return 'The request timed out. Please try again.';
  }
  if (!error?.response) {
    return 'Cannot reach the server. Please make sure the backend is running on port 8080.';
  }
  const data = error.response.data;
  const base = data?.message || data?.error || 'Something went wrong. Please try again.';
  if (data?.fieldErrors && Object.keys(data.fieldErrors).length > 0) {
    return Object.values(data.fieldErrors)[0];
  }
  return base;
}

function filenameFromDisposition(header, fallback) {
  if (!header) return fallback;
  const utf8Match = header.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match) {
    try {
      return decodeURIComponent(utf8Match[1]);
    } catch {
      return fallback;
    }
  }
  const plainMatch = header.match(/filename="?([^";]+)"?/i);
  return plainMatch ? plainMatch[1] : fallback;
}

export async function downloadFile(url, fallbackName) {
  const response = await api.get(url, { responseType: 'blob' });
  const filename = filenameFromDisposition(
    response.headers['content-disposition'],
    fallbackName,
  );
  const blobUrl = URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(blobUrl), 2000);
}

export default api;
