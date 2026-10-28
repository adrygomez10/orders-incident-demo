# Clean Architecture y dominio de pedidos

El contexto de pedidos recibe una cantidad y una referencia opcional, valida sus reglas y conserva el pedido. La instrumentación de Sentry ayuda a investigar fallos sin formar parte de esas reglas.

```text
adapter/in/web → application → domain
adapter/out/persistence → application + domain
adapter/out/telemetry → application
bootstrap → composición de las implementaciones
```

## Reglas y agregado

`Order` es la raíz del agregado y se crea mediante `place`. Su identidad es un UUID y su estado es inmutable. `OrderQuantity` protege el intervalo de una a cien unidades; `CustomerReference` normaliza espacios, acepta ausencia como cadena vacía y limita la longitud a cien caracteres. Un pedido no puede crearse con valores inválidos. No hay setters ni listas internas modificables.

Los identificadores ficticios de las cabeceras pertenecen al entorno HTTP de demostración. No se modelan como clientes de negocio. El proyecto tampoco añade líneas, pagos o estados del pedido que este caso de uso no necesita.

## Caso de uso y adaptadores

`PlaceOrder` recibe un comando, pide al agregado que cree un pedido válido y lo guarda mediante `OrderRepository`. La generación de identificadores se inyecta para hacer comprobable la operación. El resultado de aplicación contiene los datos que necesita devolver el adaptador HTTP.

`OrderController` valida las cabeceras de la demo, transforma el JSON en comando y presenta una respuesta tipada. `OrderExceptionHandler` traduce reglas rechazadas a 400 y fallos inesperados a 500. `JdbcOrderRepository` contiene exclusivamente la traducción del pedido al INSERT parametrizado.

`OrderTelemetryFilter` abre y cierra una observación por petición. Depende del contrato `OrderTelemetry`, no del SDK. El adaptador Sentry captura errores y clasifica la observación como correcta, rechazada o fallida. La composición conecta esas piezas y resuelve la configuración. Dominio y caso de uso no importan Spring, JDBC, HTTP ni Sentry.

## Pruebas y coste

Las pruebas de dominio verifican las invariantes; las de aplicación comprueban que un pedido inválido no llega a persistencia. Las pruebas HTTP verifican respuestas tipadas, cabeceras por defecto, errores y cierre de la observación. ArchUnit comprueba las dependencias hacia el interior, la separación de adaptadores y la encapsulación de campos.

Esta separación introduce contratos y traducciones. Se utiliza para probar el negocio sin base de datos y cambiar almacenamiento u observabilidad sin editarlo. No se añade un repositorio por tabla ni una interfaz por cada objeto valor.
