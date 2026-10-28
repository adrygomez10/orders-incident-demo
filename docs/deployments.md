# Despliegues y versiones

## Identificación

Cada arranque recibe `APP_RELEASE=orders-demo@<sha>` usando el SHA completo del código desplegado. La etiqueta Sentry debe corresponder al mismo commit que se consulta en GitHub.

La fecha de creación de una versión no equivale necesariamente a la hora del despliegue. El generador guarda la ventana real de peticiones; la documentación de resultados distingue ambas fechas.

Un SHA identifica el código ejecutado aunque no haya una versión anterior disponible para comparar. Las afirmaciones sobre aumentos o cambios entre despliegues necesitan datos de ambas situaciones.

## Cambios de esquema

Comparar las migraciones entre los SHAs afectados antes de atribuir un fallo a PostgreSQL. La presencia de un error de guardado no demuestra que falle una sentencia SQL: el programa puede fallar antes de ejecutarla.
