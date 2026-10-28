# Componentes del sistema de pedidos

## API de pedidos

`POST /orders` recibe `customerReference` y `quantity`. `OrderController` traduce HTTP al caso de uso `PlaceOrder`. El agregado `Order` valida `OrderQuantity` y normaliza `CustomerReference`; el repositorio guarda el pedido en PostgreSQL.

El contrato establece que `customerReference` es opcional. Los clientes `legacy-1` pueden omitirlo. Según ese contrato, una referencia ausente debe almacenarse como cadena vacía. Los clientes `web-2` suelen enviar una referencia explícita. Esta diferencia de contrato debe comprobarse al investigar fallos de guardado.

Los eventos llevan `component=order-api`, `client_version`, `environment=workshop`, identificador de usuario sintético y versión de aplicación. No representan usuarios de producción.

## Persistencia

La tabla `orders` conserva identificador, referencia y cantidad. La migración inicial no impone que la petición HTTP contenga una referencia: corresponde a la aplicación adaptar el valor opcional al formato de almacenamiento antes de guardar.
