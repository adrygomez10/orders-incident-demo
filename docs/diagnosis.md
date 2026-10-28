# Procedimiento de investigación

## Errores al guardar

Buscar primero evidencias que permitan delimitar el fallo: excepción, operación, primer evento observado, versión, componente y cliente. La fecha del primer evento observado puede ser posterior al comienzo real si se perdieron eventos.

Contrastar la traza con el código de la versión afectada. Si hay una versión anterior disponible, comparar cambios puede aportar información; si no existe, no inferir un aumento tras el despliegue ni inventar una cronología. Buscar parámetros ausentes y condiciones de los clientes afectados.

## Ventanas y medidas

La lista de incidencias agrupadas no equivale a un recuento de eventos. Una muestra limitada o truncada no representa toda la población. Una tasa necesita el número total de peticiones de la misma ventana. Si falta el denominador, comunicar recuentos disponibles y su alcance, sin inventar porcentajes.

## Incertidumbre

Si no hay eventos, código o documentación suficiente, conservar las hipótesis y proponer la comprobación que permitiría distinguirlas. No afirmar que un problema no existe porque una consulta no lo encuentra.
