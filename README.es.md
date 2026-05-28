# Juego CRPG
¡Mezcla el mundo superior de mazmorras basado en sitios de Lords of Magic
con el combate al estilo de Baldur's Gate y un toque de gestión de
resistencia (stamina) al estilo de Dark Souls!

## Distinciones clave
* Combate al estilo CRPG, pero menos indulgente
    * La posición y la rotación son muy importantes; recibir un disparo por la espalda es muy malo
    * La resistencia (stamina) lo dicta todo
    * Estilo de pausa y planificación

## Curación/Creación de recursos
Tilesets extraídos de la búsqueda de imágenes de Google

Gestión de tilesets usando Tiled
https://www.mapeditor.org/

Gestión de sprites/animaciones usando Aseprite
https://www.aseprite.org/

## Tareas actuales
- Efecto al recibir un golpe (parpadeo en blanco, algo cuando un personaje recibe daño.)
- Movimiento de cámara más suave
- Rectángulo de selección múltiple al arrastrar el ratón.
- Efectos de sonido, música de fondo.
- Ataques a distancia
- Corregir la rotación de los personajes, a veces toman el camino largo
- La IA enemiga se basa en el campo de visión (FoV)
- Cosas bonitas como nubes de polvo


## Pendientes por prioridad

### ERRORES (BUGS)

- Movimiento de cámara más suave. Al usar las teclas WASD o las flechas para mover la cámara, mueve un _punto de apoyo_ en lugar de
la posición de la cámara en sí. En su lugar, haz que la cámara _persiga el punto de apoyo_, acelerando cuanto más lejos esté del
punto de apoyo y desacelerando suavemente a medida que se acerca.

### Mejoras

### Batalla

MVP:
 x El combate se basa en la resistencia, la posición, la rotación y la velocidad
 x Como jugador, las entidades con escudos son mucho más resistentes cuando tienen los escudos en alto, pero se mueven más lento
 x Como jugador, puedo pausar el juego en cualquier momento y dar órdenes a mis entidades
 x Como jugador, mis entidades pueden abrirse camino inteligentemente a través de una mazmorra
 x Como jugador, debería ser recompensado por aprovechar los cuellos de botella en la mazmorra, colocando entidades detrás de columnas para evitar
 ataques a distancia y la línea de visión
 - Como jugador, debería tener entidades que sean más rápidas o más lentas que otras según su equipo y habilidades
 - Como jugador, debería tener armas cuerpo a cuerpo y a distancia
 - Como jugador, debería tener una mazmorra para explorar
 - Como jugador, tengo enemigos que son tanto cuerpo a cuerpo como a distancia, rápidos y lentos
 - Las entidades no deberían superponerse entre sí.
 - Como jugador, puedo encontrar botín y debo llevarlo afuera
    x Implementación: Un objeto "transportable" seguirá detrás de la entidad que lo lleva.
    - El botín cerca de un carromato se añade al carromato y será transportado afuera, el botín queda *reclamado*
 - Como jugador, el nivel de luz en una mazmorra o área juega un papel táctico importante

#### Mazmorra
* Capa de interfaz (UI)
    * Corregir las formas que muestran la dirección de los personajes
    * Corregir la línea de trayectoria
    * Pasar el ratón sobre un personaje muestra sus datos (Equipo con números)
    * Mensajes de texto ambiental (como en Nox, un bonito texto blanco flotante que desaparece. También conocido como Toasts)
* Ataques a distancia
* IA enemiga basada en el campo de visión (FOV)
* Indicadores de salud (pips) en el personaje
* Hacer que las entidades colisionen
* Los ataques se determinan al hacer clic sobre otra entidad
* Sonido
    * Goteo ambiental de agua y otros
    sonidos espeluznantes de mazmorra
    * Moverse hace ruido, provocando pequeños
    efectos gráficos que simbolizan
    que se oye algo. Esto ayuda si hacemos
    el FOV basado en entidades
##### Pulido
  - [ ] Al hacer clic en un personaje jugador (PC)
  el círculo de resistencia, el círculo de vulnerabilidad y los indicadores de vida aparecen gradualmente y se escalan al tamaño correcto.
  - [ ] Al hacer clic fuera de un PC (deseleccionar)
  el círculo de resistencia, el círculo de vulnerabilidad y los indicadores de vida se desvanecen y se encogen un poco
  - [ ] Al hacer clic para que un PC se mueva
  la línea crece a lo largo de la trayectoria; al llegar al destino, un círculo crece muy rápidamente más allá del radio objetivo y se encoge suavemente hasta el radio adecuado. El círculo parece estar girando
  - [ ] Al establecer la rotación de un PC
  Dibujar una línea desde el punto de anclaje hasta el ratón; al soltar, la línea brilla y desaparece
  - [ ] Al hacer clic en un enemigo (PC para atacar)
  Igual que la otra línea, pero con un color agresivo diferente
  - [ ] PC moviéndose
  Pequeñas nubes de polvo, ¿quizás una pequeña distorsión que represente el ruido?
  - [ ] PC y NPC atacando
  Animaciones de ataque, sonidos
  - [ ] PC y NPC recibiendo golpes
  Animaciones de daño, cambio de color a rojo, salpicaduras de sangre
  - [ ] Movimiento de cámara suavizado
  Se mueve de inmediato cuando el jugador presiona una dirección, pero desacelera un poco a medida que llega al destino.
  - [ ] Corregir el giro en las esquinas


### Escena de batalla de demostración
Mazmorra de 30 x 30
grupo:
    2 guerreros con escudos
    2 guerreros con espadón
    2 guerreros con arcos
    4 "exploradores" (rápidos, sin armadura ni escudos, poco daño)

La ubicación inicial tiene una fogata y tiendas de campaña

La mazmorra tiene:
    Estos enemigos:
        goblins: Rápidos pero muy débiles. Te matarán si te toman por sorpresa
        orcos con escudo: Tienen escudos, daño moderado. Pueden mantener la línea
        orcos con hacha: Hachas grandes, sin escudos. Pueden ser peligrosos a menos que se les contenga con un escudo
        orcos arqueros: Disparan flechas
        gran orco: Hacha grande, escudo grande, debe ser rodeado para despacharlo con seguridad. Múltiples indicadores de salud.
    Y estas características de mazmorra:
        Una gran sala abierta
        Un pasillo estrecho con esquinas aterradoras
        Una sala con muchos pilares
        Tesoro en varias salas
        Una sala que es un pasillo circular
        Gran sala del jefe con el gran orco y un gran tesoro
    Los jugadores deberían encontrar (`probablemente solo con texto ambiental por ahora`)
        Monedas
        Gemas
        Una gran estatua
        Un mapa a otra ubicación
        Una veta de mineral
        Un manantial de agua dulce
        Escritos arcanos que es necesario copiar
### Mundo superior (Overworld)
* La escena del mapa muestra
* Colocar el grupo
* Mover el grupo
    * BAJA: Mover el grupo agota la resistencia
    * BAJA: Necesita descansar cuando la resistencia se agota
    * BAJA: La resistencia se agota más lentamente cerca de
    territorio familiar.
* Poblar con características
  * Bosques
  * Montañas
  * Llanuras
  * Ríos
* Poblar con sitios aleatorios
Los sitios son amistosos u hostiles.

Si un sitio es amistoso y tiene recursos valiosos cerca
(cosas que se descubrieron durante una aventura), entonces
los pueblos construirán caminos hacia él. Una vez que el camino conecta
con el sitio, obtiene acceso a esos recursos.
  * Mazmorras
  * Pueblos
    * ¿Caminos entre pueblos?


#### Futuro
Caminos más cortos conducen a una extracción más eficiente (¿mercados más baratos?)

Si un camino se ve amenazado, entonces el recurso puede desaparecer
o volverse más caro.

* Según los recursos llevados a un pueblo, hay diferentes bienes disponibles
para comprar en el pueblo
* ¡Por cierto, los pueblos tienen mercados!


### Pensamientos aleatorios


## Futuro
* Magia
    * ¿Lanzar un hechizo agota toda la resistencia?

* ¡Sistema de subida de nivel!
    * Para la demostración
        * 1000 puntos por nivel
        * Aumenta la resistencia
        * El goblin da 50 de experiencia
        * El orco da 100 de experiencia
        * El gran orco da 300 de experiencia

## Quizás/Futuro

* Mecánicas de rotación
  * Al moverse, diferentes tipos de velocidades afectan cómo rotas. Si ordenas a un PC moverse a un punto, puede o no
  necesitar rotar hacia ese punto según la velocidad a la que se mueva.
    * Corriendo: Puede rotar en cualquier dirección dentro de un arco de 180 grados centrado en la dirección de la carrera.
    Velocidad de movimiento predeterminada.
      * Ejemplos: Correr y disparar
    * Esprintar/Cargar: Puede rotar en cualquier dirección dentro de un arco de 30-45 grados centrado en la dirección del esprint.
      * Ejemplo: ¡Cargar! Moverse así quema resistencia.
  * PENDIENTE: Diferentes velocidades de movimiento
  * PENDIENTE: Rotar según las reglas de velocidad de movimiento

## HECHO

#### Botín
* Botín y "Mulas"
    * Las mulas son personajes no combatientes que
    se mueven rápido y pueden cargar botín. También podrían ser personajes
    jugadores.
    * El botín será un objeto seleccionable que los PC pueden recoger
    * Para "ganar/reclamar" el botín, debe llevarse de vuelta al "campamento"
    * Parte del botín no son materiales reales. Los personajes también pueden encontrar
    "cosas de interés" como vetas de mineral, manantiales de agua dulce, escritos
    arcanos, y pergaminos/mapas a otras ubicaciones y conocimientos.
    `para la demo esto puede ser solo texto ambiental`

#### Transporte (Hauling)
[x] Implementar el sistema de transporte. Algo que se transporta sigue detrás del transportador.
    - Al hacer clic en un transportador y luego en algo transportable dentro del alcance, se adjuntará
    la entidad transportable al transportador. Lo seguirá si el transportador se mueve
    - Al hacer clic en una entidad transportable adjunta, se desadjunta del transportador
    - Cosas que podrían transportarse:
        - Botín
        - NPC abatidos
[x] *Requiere el transporte implementado* Cuando un NPC se queda sin puntos de vida, ahora está "ABATIDO" y comienza un temporizador circular. Cuando se acaba el tiempo,
mueren para siempre. Los enemigos atacarán otros objetivos a menos que ese sea el único objetivo dentro del alcance. El NPC abatido no puede moverse
ni hacer nada más. Sin embargo, se vuelve `transportable`, y cuando un NPC amistoso empieza a `transportar` al personaje abatido
la cuenta atrás se ralentiza drásticamente. Eventualmente podrán ser restaurados si son transportados a un sanador.

#### Campo de visión (FoV)
* Línea de visión
    * Referencias
        * https://www.redblobgames.com/articles/visibility/
        * https://ncase.me/sight-and-light/
        * http://saltares.com/blog/games/2d-vision-system-with-ashley-and-box2d/
    * Necesidad de convertir tiles en polígonos
    * Encontrar todos los polígonos en el radio de visión de cada unidad y enemigo
    * Tomar todas las líneas de los polígonos
    * Ejecutar los algoritmos anteriores
    * ¿Campo de visión basado en la dirección
    que mira la entidad?
* Optimizar el FOV
    1. optimizar los polígonos de los tiles
    2. Paralelizar los cálculos de polígonos (INNECESARIO)
