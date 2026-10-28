# Orders Incident Demo

API Java de pedidos con PostgreSQL y captura opcional de errores y trazas en Sentry. Aporta el servicio, el código y la documentación que investiga el proyecto hermano `incident-investigator`. Los usuarios y pedidos son ficticios.

**Esta copia es la base funcional:** un pedido sin `customerReference` se guarda con referencia vacía y devuelve 201. Con una referencia explícita también devuelve 201. El lote de treinta peticiones debe guardar treinta pedidos y no producir errores 500.

## Requisitos y carpetas

Necesitas Docker con contenedores Linux y Compose v2, Git y curl. Los comandos utilizan **Bash**, incluido WSL en Windows. Python 3.12 o posterior solo es necesario para generar lotes y ejecutar los scripts de comprobación; no requiere instalar paquetes Python. Java y Maven se ejecutan dentro de la imagen.

```text
proyectos/
├── orders-incident-demo/
└── incident-investigator/
```

La API de pedidos funciona sola con su PostgreSQL: no necesita el investigador ni una cuenta Sentry para guardar pedidos. Para el recorrido completo sigue después el [README del investigador](../incident-investigator/README.md).

## 1. Crear la configuración local

Desde esta carpeta:

```bash
test -f .env || cp .env.example .env
```

Edita **`.env`**, no `.env.example`:

| Variable | Qué configurar |
| --- | --- |
| `DB_PASSWORD` | Contraseña local de PostgreSQL; elige una al crear el volumen y consérvala |
| `APP_RELEASE` | `orders-demo@local-review` para probar HTTP sin historial; para investigar con GitHub utiliza `orders-demo@<SHA completo>` |
| `SENTRY_DSN` | Vacío para probar solo HTTP y PostgreSQL; DSN de tu proyecto para enviar eventos |
| `DEMO_RUN` | Etiqueta del lote, por ejemplo `local-check-01` |

Puedes generar la contraseña con `python3 -c 'import secrets; print(secrets.token_hex(32))'`. `.env` está excluido de Git y de la imagen; la plantilla debe conservar únicamente ejemplos.

Para identificar la revisión de un checkout Git:

```bash
git rev-parse HEAD
```

Copia su SHA completo en `APP_RELEASE` después de `orders-demo@`. Si todavía no hay commits o descargaste un ZIP, conserva la etiqueta local y prueba HTTP; esa etiqueta no identifica un commit que el agente pueda consultar en GitHub. La investigación completa necesita una revisión accesible en el repositorio configurado y el corpus exportado de esa misma revisión. Un árbol con cambios sin commit tampoco corresponde exactamente al SHA de `HEAD`.

## 2. Arrancar y comprobar la salud

```bash
docker compose up --build -d
docker compose ps
curl -fsS http://127.0.0.1:18091/actuator/health
```

Espera a que la salud contenga `"status":"UP"`. Compose arranca PostgreSQL, espera a que acepte conexiones y ejecuta la aplicación; Flyway prepara la tabla `orders`. La base conserva sus datos en un volumen Docker y no expone el puerto al equipo anfitrión.

**Abrir `http://127.0.0.1:18091/` devuelve 404:** no hay interfaz gráfica ni página de inicio. Para enviar un pedido hay que hacer un POST, no abrir `/orders` con un GET en el navegador.

## 3. Enviar pedidos

Con referencia explícita:

```bash
curl -i -X POST http://127.0.0.1:18091/orders \
  -H 'Content-Type: application/json' \
  -H 'X-Demo-User: demo-user-1' \
  -H 'X-Client-Version: web-2' \
  -d '{"quantity":1,"customerReference":"  ORDER-1  "}'
```

Devuelve **201** y un JSON con `id` y `customerReference` normalizado a `ORDER-1`. Los UUID cambian en cada petición.

Sin referencia opcional:

```bash
curl -i -X POST http://127.0.0.1:18091/orders \
  -H 'Content-Type: application/json' \
  -H 'X-Demo-User: demo-user-2' \
  -H 'X-Client-Version: legacy-1' \
  -d '{"quantity":1}'
```

En esta base funcional devuelve **201** con `customerReference` vacío. Cantidades fuera de 1 a 100, referencias mayores de cien caracteres o cabeceras inválidas devuelven 400. `X-Demo-User` admite `demo-user-<número>`; `X-Client-Version`, entre uno y treinta caracteres alfanuméricos, puntos, guiones o guiones bajos. Si omites las cabeceras, se usan los valores de demostración por defecto.

## 4. Conectar Sentry

Sigue [la configuración de Sentry](../incident-investigator/docs/services.md#sentry-recibir-eventos-desde-pedidos) del proyecto investigador: crea un proyecto Java, habilita Error monitoring y Tracing y guarda su DSN en **este** `.env`, en `SENTRY_DSN`. Si solo descargaste pedidos, puedes crear el proyecto y obtener el DSN en sus ajustes **Client Keys (DSN)**; el SDK ya está integrado y no necesitas instalar plugins.

Para aplicar el DSN o cambiar `APP_RELEASE`/`DEMO_RUN`:

```bash
docker compose up -d --force-recreate app
```

Repite una petición y busca en Sentry la traza **`POST /orders`**, entorno **`workshop`**, proyecto e intervalo correctos. Una petición correcta genera una traza; no crea una incidencia en Issues. Los errores inesperados, cuando se produzcan, devuelven 500 y llevan versión, componente `order-api`, cliente, usuario ficticio y etiqueta de lote. La ausencia de excepciones en esta base funcional es un resultado esperado.

El DSN envía datos a Sentry. El token de lectura que usará el investigador se configura en el otro proyecto y tiene una función distinta.

## 5. Generar un lote medible

```bash
python3 scripts/generate_traffic.py --requests 30 --output reports/traffic.json
```

Diez peticiones omiten la referencia y veinte la incluyen. En esta base funcional se esperan **treinta respuestas 201 y cero errores 500**. `reports/traffic.json` conserva el total, cada estado y la ventana UTC que puedes pasar a la investigación. El generador admite entre una y cien peticiones por ejecución y no elimina pedidos anteriores. `reports/` está excluido de Git.

El siguiente paso es [configurar y arrancar el investigador](../incident-investigator/README.md), exportar el corpus de este commit y encargar la investigación. Configurar Sentry no implica que un modelo haya investigado todavía ni garantiza acceso desde el MCP.

## Parar y resolver problemas

`docker compose down` detiene el servicio y conserva los pedidos. Para reconstruir después de editar código usa `docker compose up --build -d`. Para cambiar variables usa `docker compose up -d --force-recreate app`.

| Síntoma | Qué comprobar |
| --- | --- |
| Conexión rechazada | `docker compose ps` y `docker compose logs --tail 80 app db` |
| 404 en `/` o 405 en `/orders` | Usa salud o los POST anteriores; no hay página web |
| Falta `DB_PASSWORD` | Completa `.env` antes del arranque |
| Contraseña inválida después de cambiar `.env` | El volumen existente mantiene la contraseña inicial; recupera esa configuración o cambia la contraseña en PostgreSQL |
| Puerto 18091 ocupado | Detén la otra aplicación que utilice ese puerto o adapta el mapeo de Compose y la URL de tus peticiones |
| Sentry vacío | DSN, recreación de `app`, petición posterior al cambio y filtros de proyecto/entorno/fecha; consulta Traces para peticiones correctas |
| `HEAD` no existe | Usa `orders-demo@local-review` para HTTP; el contraste con GitHub necesita un checkout con commits accesibles |

## Documentos y comprobaciones

Los Markdown de `docs/` son el **corpus del investigador**: componentes, contrato, decisiones, procedimiento, despliegues y un antecedente ficticio. Por eso se conservan aunque no sean pasos de instalación. Las respuestas esperadas del escenario y los informes permanecen fuera de ese corpus.

- [ADR de normalización](docs/adr-normalization.md).
- [Arquitectura de pedidos](docs/architecture.md).

Para modificar Java necesitas JDK 25. Desde esta carpeta, `./mvnw verify` ejecuta pruebas de dominio, aplicación, HTTP y ArchUnit, además de Spotless y Checkstyle con el perfil Google. La revisión manual completa nombres, contratos y separación lógica. Maven Wrapper fija la versión de Maven.

```bash
./mvnw verify
python3 -m pip install -r requirements-dev.txt
python3 -m ruff check .
python3 -m ruff format --check .
python3 -m unittest discover -s src/test/python
```

Ruff comprueba las convenciones PEP 8 y PEP 257. Estas dependencias de desarrollo no hacen falta para generar tráfico.

Para comprobar HTTP y persistencia en una base desechable, sin usar tu `.env` ni enviar eventos Sentry:

```bash
python3 scripts/verify_scenario.py
```

El script construye la copia actual, comprueba treinta respuestas 201, treinta filas y ejecución como UID 10001, y retira sus contenedores y red. Exige un checkout con `HEAD` y sin cambios locales para identificar correctamente el código. Puedes usar las peticiones manuales anteriores durante una revisión sin commits.
