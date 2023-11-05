For the texture issue, the draw call at line 1061 in TextureArrayCpuPolygonSpriteBatch.java
is the first entry

then line 394

then 1061 again, 394

Repeats forever cause that's the update loop
