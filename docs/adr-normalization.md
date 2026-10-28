# Decisión: normalización en la entrada

## Problema

Los clientes envían referencias con espacios o sin valor. La capa de persistencia requiere una cadena no nula.

## Decisión

Normalizar antes de guardar. La operación debe aceptar `null`, devolver cadena vacía para la ausencia y retirar espacios exteriores cuando hay texto. Se conserva el comportamiento de los clientes antiguos.

## Alternativas y coste

Rechazar peticiones sin referencia simplifica la base de datos, pero rompe el contrato de clientes existentes. Permitir nulos en almacenamiento traslada la distinción a todas las lecturas. La normalización concentra esa política en un método que necesita pruebas con ausencia y texto explícito.
