import http from 'k6/http';
import { check, sleep } from 'k6';

// Etapa 11: no probamos login bajo carga a propósito -- el rate limiter de
// la Etapa 10 (5 intentos/60s por username) haría que la mayoría de los
// VUs reciban 429 enseguida, que es el comportamiento CORRECTO del rate
// limiter, no degradación del sistema bajo carga. setup() se loguea UNA
// sola vez y todos los VUs reusan ese mismo token -- así medimos lo que
// esta etapa realmente quiere probar: el camino de lectura (calendario y
// posiciones, cacheados en Redis) repartido entre las dos instancias
// detrás de nginx.
export const options = {
  scenarios: {
    ramping_reads: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 20 },
        { duration: '40s', target: 50 },
        { duration: '20s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<800'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  const username = `k6-${Date.now()}`;
  const password = 'claveSegura123';

  const registerRes = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ firstName: 'K6', lastName: 'Load', username, password }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  if (registerRes.status !== 200) {
    throw new Error(`setup: registro falló con ${registerRes.status}: ${registerRes.body}`);
  }

  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username, password }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  if (loginRes.status !== 200) {
    throw new Error(`setup: login falló con ${loginRes.status}: ${loginRes.body}`);
  }

  return { token: loginRes.json('accessToken') };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}` };

  const matchesRes = http.get(`${BASE_URL}/api/matches?page=0&size=20`, { headers });
  check(matchesRes, { 'matches -> 200': (r) => r.status === 200 });

  const standingsRes = http.get(`${BASE_URL}/api/standings?page=0&size=20`, { headers });
  check(standingsRes, { 'standings -> 200': (r) => r.status === 200 });

  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, { 'health -> 200': (r) => r.status === 200 });

  sleep(1);
}
