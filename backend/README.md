# Backend de PasajeApp

API REST de la HU 3.1 construida con Express, TypeScript y PostgreSQL. Android se comunica exclusivamente con esta API; las credenciales de PostgreSQL solo se leen desde el entorno del proceso backend.

## Preparación

1. Copia `.env.example` como `.env`.
2. Reemplaza el valor de ejemplo de la variable `DATABASE_URL` por la conexión rotada de Neon.
3. Mantén `sslmode=require` en la conexión.
4. Instala las dependencias con `npm install`.
5. Ejecuta `npm run migrate`.
6. Inicia en desarrollo con `npm run dev`.

El archivo `.env` está ignorado por Git. La API no registra ni devuelve la URL de conexión.

## Comandos

- `npm run dev`: servidor con recarga.
- `npm run build`: validación y compilación TypeScript.
- `npm start`: servidor compilado.
- `npm test`: pruebas unitarias y HTTP.
- `npm run migrate`: migración controlada e idempotente.

La ruta `GET /health` consulta la base de datos. Solo responde `status: ok` cuando la API puede comunicarse realmente con PostgreSQL.
