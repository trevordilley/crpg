> 🌐 [English](README.md) | [Español](README.es.md)

# Juego CRPG
¡Mezcla el supramundo de mazmorras basado en sitios de Lords of Magic
con el combate al estilo de Baldur's Gate y un giro de gestión de
resistencia al estilo de Dark Souls!

## Distinciones clave
* Combate al estilo CRPG, pero menos indulgente
    * La posición y la rotación son muy importantes; que te disparen por la espalda es muy malo
    * La resistencia lo dicta todo
    * Estilo de pausar y planificar

## Curación/Creación de recursos
Conjuntos de tiles extraídos de la búsqueda de imágenes de Google

Gestión de conjuntos de tiles usando Tiled
https://www.mapeditor.org/

Gestión de sprites/animaciones usando Aseprite
https://www.aseprite.org/

## Tareas actuales
- Efecto al recibir golpe (destello blanco, algo cuando un personaje resulta dañado).
- Movimiento de cámara más suave
- Selección múltiple con rectángulo al arrastrar el ratón.
- Efectos de sonido, música de fondo.
- Ataques a distancia
- Corregir la rotación de personajes; a veces toman el camino largo
- La IA enemiga se basa en el campo de visión (FoV)
- Detalles bonitos como nubes de polvo

## Pendientes por prioridad

### ERRORES (BUGS)

- Movimiento de cámara más suave. Al usar las teclas WASD o las flechas para mover la cámara, mueve un _punto de apoyo_
en lugar de la posición de la propia cámara. En cambio, haz que la cámara _persiga el punto de apoyo_, acelerando cuanto
más lejos esté del punto de apoyo y desacelerando suavemente a medida que se acerca.

### Mejoras

### Batalla

MVP (Producto Mínimo Viable):
 x El combate se basa en la resistencia, la posición, la rotación y la velocidad
 x Como jugador, las entidades con escudos son mucho más resistentes cuando sus escudos están en alto, pero se mueven más lento
 x Como jugador, puedo pausar el juego en cualquier momento y dar órdenes a mis entidades
 x Como jugador, mis entidades pueden encontrar su camino de forma inteligente a través de una mazmorra
 x Como jugador, debería ser recompensado por aprovechar los cuellos de botella en la mazmorra, colocando entidades detrás de
 columnas para evitar ataques a distancia y la línea de visión
 - Como jugador, debería tener entidades que sean más rápidas o más lentas que otras según su equipo y habilidades
 - Como jugador, debería tener armas cuerpo a cuerpo y a distancia
 - Como jugador, debería tener una mazmorra que explorar
 - Como jugador, tengo enemigos que son tanto cuerpo a cuerpo como a distancia, rápidos y lentos
 - Las entidades no deberían superponerse entre sí.
 - Como jugador, puedo encontrar botín y debo sacarlo
    x Implementación: Una cosa "transportable" seguirá detrás de la entidad que la lleva.
    - El botín cerca de un carro se añade al carro y será transportado fuera; el botín queda *reclamado*
 - Como jugador, el nivel de luz en una mazmorra o área juega un papel táctico importante

#### Mazmorra
* Capa de interfaz (UI)
    * Corregir las formas que muestran la dirección de los personajes
    * Corregir la línea de pathing (ruta)
    * Pasar el ratón sobre un personaje muestra sus datos (equipo con números)
    * Mensajes de texto descriptivo (como en Nox, texto blanco flotante agradable que desaparece. También conocidos como Toasts)
* Ataques a distancia
* IA enemiga basada en el campo de visión (FOV)
* Indicadores de salud (pips) en el personaje
* Hacer que las entidades colisionen
* Los ataques se determinan al hacer clic en otra entidad
* Sonido
    * Gotas ambientales de agua y otros
    sonidos espeluznantes de mazmorra
    * Moverse hace ruido, provocando pequeños
    efectos gráficos que simbolizan
    que algo se oye. Esto ayuda si hacemos
    el FOV basado en entidades
##### Pulido
  - [ ] Al hacer clic en un PJ (personaje jugador)
  el círculo de resistencia, el círculo de vulnerabilidad y los pips de vida aparecen con fundido y escalan al tamaño correcto.
  - [ ] Al hacer clic fuera de un PJ (deseleccionar)
  el círculo de resistencia, el círculo de vulnerabilidad y los pips de vida se desvanecen y encogen un poco
  - [ ] Al hacer clic para que un PJ se mueva
  una línea crece a lo largo de la ruta; al llegar al destino, un círculo crece muy rápido más allá del radio objetivo y se encoge suavemente al radio adecuado. El círculo parece estar girando
  - [ ] Al establecer la rotación de un PJ
  Dibujar una línea desde el punto de anclaje hasta el ratón; al soltar, la línea brilla y desaparece
  - [ ] Al hacer clic en un enemigo (para que el PJ ataque)
  Igual que la otra línea, pero con un color agresivo diferente
  - [ ] PJ moviéndose
  Pequeñas nubes de polvo, ¿quizás una pequeña distorsión que represente el ruido?
  - [ ] PJ y PNJ atacando
  Animaciones de ataque, sonidos
  - [ ] PJ y PNJ recibiendo golpes
  Animaciones de daño, cambio de color a rojo, salpicaduras de sangre
  - [ ] Movimiento de cámara suavizado
  Se mueve inmediatamente cuando el jugador presiona una dirección, pero desacelera un poco al acercarse al destino.
  - [ ] Corregir el paso por las esquinas

### Escena de batalla de demostración
Mazmorra de 30 x 30
grupo:
    2 guerreros con escudos
    2 guerreros con espadón
    2 guerreros con arcos
    4 "exploradores" (rápidos, sin armadura ni escudos, poco daño)

La ubicación inicial tiene una hoguera y tiendas

La mazmorra tiene:
    Estos enemigos:
        goblins: Rápidos pero muy débiles. Te matarán si te sorprenden por la espalda
        orcos con escudo: Tienen escudos, daño moderado. Pueden mantener la línea
        orcos con hacha: Hachas grandes, sin escudos. Pueden ser peligrosos a menos que se les contenga con un escudo
        orcos arqueros: Disparan flechas
        orco grande: Hacha grande, escudo grande, debe ser rodeado para despacharlo con seguridad. Múltiples pips de salud.
    Y estas características de mazmorra:
        Sala grande y abierta
        Pasillo estrecho con esquinas aterradoras
        Sala con muchos pilares
        Tesoro en varias salas
        Sala que es un pasillo circular
        Gran sala de jefe con el orco grande y un gran tesoro
    Los jugadores deberían encontrar (`probablemente solo con texto descriptivo por ahora`)
        Monedas
        Gemas
        Estatua grande
        Mapa a otra ubicación
        Veta de mineral
        Manantial de agua fresca
        Escritos arcanos que necesitan ser copiados
### Supramundo
* La escena del mapa muestra
* Colocar el grupo
* Mover el grupo
    * BAJA: Mover el grupo agota la resistencia
    * BAJA: Necesita descansar cuando la resistencia se agota
    * BAJA: La resistencia se agota más lento cerca de
    territorio familiar.
* Poblar con características
  * Bosques
  * Montañas
  * Llanuras
  * Ríos
* Poblar con sitios aleatorios
Los sitios son amistosos o no amistosos.

Si un sitio es amistoso y tiene recursos valiosos cerca
(cosas que se descubrieron durante una aventura), entonces
los pueblos construirán caminos hacia él. Una vez que el camino se conecta
al sitio, obtiene acceso a esos recursos.
  * Mazmorras
  * Pueblos
    * ¿Caminos entre pueblos?


#### Futuro
Los caminos más cortos conducen a una extracción más eficiente (¿mercados más baratos?)

Si un camino se ve amenazado, entonces el recurso puede desaparecer
o volverse más caro.

* Según los recursos llevados a un pueblo, hay diferentes bienes
disponibles para comprar en el pueblo
* ¡Por cierto, los pueblos tienen mercados!


### Pensamientos aleatorios


## Futuro
* Magia
    * ¿Lanzar un hechizo agota toda la resistencia?

* ¡Sistema de subida de nivel!
    * Para la demo
        * 1000 puntos por nivel
        * Aumenta la resistencia
        * El goblin da 50 de experiencia (xp)
        * El orco da 100 xp
        * El orco grande da 300 xp

## Quizás/Futuro

* Mecánicas de rotación
  * Al moverse, diferentes tipos de velocidades afectan cómo rotas. Si ordenas a un PJ moverse a un punto, puede que necesite
  o no rotar hacia ese punto según la velocidad a la que se mueva.
    * Corriendo: Puede rotar en cualquier dirección dentro de un arco de 180 grados centrado en la dirección de la carrera.
    Velocidad de movimiento por defecto.
      * Ejemplos: Correr y disparar (running and gunning)
    * Esprintar/Cargar: Puede rotar en cualquier dirección dentro de un arco de 30-45 grados centrado en la dirección del esprint.
      * Ejemplo: ¡Cargar! Correr así quema resistencia.
  * PENDIENTE: Diferentes velocidades de movimiento
  * PENDIENTE: Rotar según las reglas de velocidad de movimiento

## HECHO

#### Botín
* Botín y "Mulas"
    * Las mulas son personajes no combatientes que
    se mueven rápido y pueden cargar botín. También podrían ser personajes
    jugadores.
    * El botín será un objeto seleccionable que los PJ pueden recoger
    * Para "ganar/reclamar" el botín, debe ser llevado de vuelta al "campamento"
    * Algo del botín no son materiales reales. Los personajes también pueden encontrar
    "cosas de interés" como vetas de mineral, manantiales de agua fresca, escritos arcanos
    y pergaminos/mapas a otras ubicaciones y conocimiento.
    `para la demo esto puede ser solo texto descriptivo`

#### Transporte (Hauling)
[x] Implementar el sistema de transporte. Algo que se transporta sigue detrás del transportador.
    - Al hacer clic en un transportador y luego en algo transportable dentro del rango, se adjuntará
    la entidad transportable al transportador. La seguirá si el transportador se mueve
    - Al hacer clic en una entidad transportable adjunta, se desvinculará del transportador
    - Cosas que podrían transportarse:
        - Botín
        - PNJ caídos
[x] *Requiere el transporte implementado* Cuando un PNJ se queda sin puntos de vida, ahora está "CAÍDO" y comienza un temporizador
circular. Cuando se acaba el tiempo, muere definitivamente. Los enemigos atacarán a otros objetivos a menos que ese sea el único
objetivo dentro del rango. El PNJ caído no puede moverse ni hacer nada más. Sin embargo, se vuelve `transportable`, y cuando un
PNJ amistoso comienza a `transportar` al personaje caído, la cuenta atrás se ralentiza drásticamente. Eventualmente podrán ser
restaurados si son transportados a un sanador.

#### Campo de visión (FoV)
* Línea de visión
    * Referencias
        * https://www.redblobgames.com/articles/visibility/
        * https://ncase.me/sight-and-light/
        * http://saltares.com/blog/games/2d-vision-system-with-ashley-and-box2d/
    * Necesita convertir los tiles en polígonos
    * Encontrar todos los polígonos en el radio del sitio de cada unidad y enemigo
    * Tomar todas las líneas de los polígonos
    * Ejecutar los algoritmos anteriores
    * ¿Campo de visión basado en la dirección
    a la que mira la entidad?
* Optimizar el FOV
    1. optimizando los polígonos de tiles
    2. Paralelizar los cálculos de polígonos (INNECESARIO)
