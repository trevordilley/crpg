package com.ancient.game.crpg.assetManagement.aseprite

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Array
import ktx.collections.toGdxArray

class AnimationSlice(
        val frameDuration: Float,
        private val keyFrames: Array<AsepriteBound> = Array(),
        private val playMode: Animation.PlayMode = Animation.PlayMode.NORMAL
) {

    fun slice(time: Float): AsepriteBound {
        return keyFrames[getKeyFrameIndex(time)]
    }

    /** Returns the current frame number.
     * @param stateTime
     * @return current frame number
     */
    fun getKeyFrameIndex(stateTime: Float): Int {
        if (keyFrames.size == 1) return 0

        var frameNumber = (stateTime / frameDuration).toInt()
        when (playMode) {
            Animation.PlayMode.NORMAL -> frameNumber = Math.min(keyFrames.size - 1, frameNumber)
            Animation.PlayMode.LOOP -> frameNumber %= keyFrames.size
            Animation.PlayMode.LOOP_PINGPONG -> {
                frameNumber %= (keyFrames.size * 2 - 2)
                if (frameNumber >= keyFrames.size) frameNumber = keyFrames.size - 2 - (frameNumber - keyFrames.size)
            }
            Animation.PlayMode.LOOP_RANDOM -> TODO("Not implemetend")
            Animation.PlayMode.REVERSED -> frameNumber = Math.max(keyFrames.size - frameNumber - 1, 0)
            Animation.PlayMode.LOOP_REVERSED -> {
                frameNumber %= keyFrames.size
                frameNumber = keyFrames.size - frameNumber - 1
            }
        }

        return frameNumber
    }


}

operator fun AsepriteSourceSize.component1() = this.w
operator fun AsepriteSourceSize.component2() = this.h

class AnimationSlices(parent: Aseprite, val slice: AsepriteSlices) {

    private var animations = emptyMap<String, AnimationSlice>()

    companion object {
        val emptyBound = AsepriteBound(0, 0, 0, 0)
    }

    init {

        val animationNames = parent.animationNames()
        val framesToIgnore = slice.data.split(",")
                .filter { it.isNotBlank() }
                .map { it.toInt() - 1 }

        val asFrameIndexedMap = parent.json.asFrameIndexedMap()

        val allSlices = Array<AsepriteBound>(parent.json.frames.size)
        var prec = slice.keys.firstOrNull()
        for (f in 1..parent.json.frames.size) {
            prec = slice.keys.find { it.frame == f } ?: prec
            prec?.run {

                val asepriteFrame = asFrameIndexedMap[f - 1]!!
                val (_, sizeY) = asepriteFrame.sourceSize

                val computedBound = this.bounds.let {
                    AsepriteBound(it.x, sizeY - it.y - it.h, it.w, it.h)
                }

                allSlices.add(computedBound)
            }
        }
        for (name in animationNames) {
            val anim = parent[name].animation
            val nbFrame = anim.keyFrames.size
            val slices = Array<AsepriteBound>(nbFrame)

            val offset = parent.json.meta.frameTags
                    .firstOrNull { it.name == name }
                    ?.let { it.from to it.to }
                    ?: 0 to 0


            for (i in offset.first..offset.second) {
                // convert milli into seconds
                val asepriteFrame = asFrameIndexedMap[i]!!
                val duration = asepriteFrame.duration.toFloat() / 1000

                val nbCopy = Math.max(1, Math.round(duration / anim.frameDuration))

                val slice = if (framesToIgnore.contains(i)) {
                    emptyBound
                } else {
                    allSlices[i]
                }
                for (nb in 1..nbCopy) {
                    slices.add(slice)
                }
            }

            animations = animations + (name to AnimationSlice(
                    anim.frameDuration,
                    slices,
                    anim.playMode
            ))
        }

    }


    operator fun get(name: String) = animations[name] ?: TODO("Animation slice not found $name")

}

class AsepriteAnimation(val animation: Animation<TextureRegion>, val frameTags: Array<Set<String>>) {
    private val allTags = frameTags.flatten().toSet()
    fun hasTag(tag: String): Boolean = allTags.contains(tag)
    fun hasTag(tag: String, frame: Int) = frameTags[frame].contains(tag)

    fun frame(elapsed: Float, looping: Boolean = false) = animation.getKeyFrame(elapsed, looping)
    fun frameIndex(elapsed: Float, looping: Boolean = false) = animation.getKeyFrameIndex(elapsed)
    fun frame(index: Int) = animation.keyFrames[index]
    fun tags(elapsed: Float) = animation.getKeyFrameIndex(elapsed).let { idx -> this.tags(idx) }
    fun tags(frame: Int) = frameTags[frame]
}

class Aseprite(private val texture: Texture, val json: AsepriteJson) {

    private val animationCache: Map<String, AsepriteAnimation>
    private val slicesCache: Map<String, AnimationSlices>
    private val frameTagsCache: Array<Set<String>>
    private val tagsCache: Set<String>

    init {
        frameTagsCache = toFrameTags()
        animationCache = toAnimation()
        slicesCache = toSlices()
        tagsCache = frameTagsCache.flatten().toSet()
    }

    operator fun get(key: String): AsepriteAnimation = animationCache[key] ?: TODO(
            "Wrong animation name: $key. Expected: ${animationCache.keys.joinToString(",")}"
    )


    fun slices(name: String) = animatedSlices(name).slice

    fun animatedSlices(name: String) = slicesCache[name] ?: invalidSlice(name)

    private fun invalidSlice(name: String): Nothing = TODO(
            "Invalid slice name $name. Other candidates : ${json.meta.slices.map { it.name }}")

    private fun toSlices(): Map<String, AnimationSlices> {
        return json.meta
                .slices
                .map { it.name to it.toAnimationSlices(this) }
                .toMap()
    }

    private fun toAnimation(): Map<String, AsepriteAnimation> {

        val spriteDef: AsepriteJson = json
        val texture: Texture = texture

        // all frame should have the same size
        val frameData = spriteDef.asFrameIndexedMap()
        val size = frameData.entries.map { it.value.sourceSize }
                .first()

        val splitted = TextureRegion.split(texture, size.w, size.h)


        return spriteDef.meta.frameTags.map {
            // trouver denominateur commun
            val keys = frameData.filterKeys { key -> key in it.from..it.to }
            val allDurations = keys.map { it.value.duration }.sorted()

            val denominateur = if (allDurations.distinct().size > 1) {
                val (a, b) = allDurations.distinct()
                a.gcd(b)
            } else {
                allDurations.first()
            }

            // sum des durations / denominateur = nb image
            val tmp = com.badlogic.gdx.utils.Array<TextureRegion>(allDurations.sum() / denominateur)
            val tags = Array<Set<String>>((it.to - it.from) + 1)
            for (index in it.from..it.to) {
                val duration = frameData[index]?.duration ?: 0
                val (x, y) = splittedIndex(index, splitted)
                for (nbCopie in 1..duration / denominateur) {
                    tmp.add(splitted[x][y])
                }
                tags.add(frameTagsCache[index])
            }
            val direction = when (it.direction) {
                "forward" -> {
                    if (it.name.endsWith("_nr")) {
                        Animation.PlayMode.NORMAL
                    } else {
                        Animation.PlayMode.LOOP
                    }
                }
                "pingpong" -> Animation.PlayMode.LOOP_PINGPONG
                else -> TODO("compute other play mode")
            }
            val animation =
                    AsepriteAnimation(
                            Animation(denominateur / 1000f, tmp, direction), tags)
            it.name to animation
        }.toMap()
    }

    private fun toFrameTags(): Array<Set<String>> =
            mutableListOf<MutableSet<String>>()
                    .apply {
                        json.frames.forEach { add(mutableSetOf()) }
                        json.meta.frameTags.forEach { tag ->
                            for (i in tag.from..tag.to) {
                                get(i).add(tag.name)
                            }
                        }
                    }
                    .run {
                        map { it.toSet() }
                    }
                    .toGdxArray()


    fun splittedIndex(index: Int, splitted: kotlin.Array<kotlin.Array<TextureRegion>>): Pair<Int, Int> {
        val x = ((index - index % splitted[0].size) / splitted[0].size)
        val y = index % splitted[0].size
        return Pair(x, y)
    }

    fun frame(i: Int): TextureRegion {
        val frameData = json.asFrameIndexedMap()
        val size = frameData.entries.map { it.value.sourceSize }
                .first()

        val splitted = TextureRegion.split(texture, size.w, size.h)
        val (x, y) = splittedIndex(i, splitted)
        return splitted[x][y]
    }

    fun animationNames() = animationCache.map { it.key }

    val width: Int get() = frame(0).regionWidth
}


class AsepriteParameter : AssetLoaderParameters<Aseprite>()

class AsepriteLoader(resolver: FileHandleResolver) : AsynchronousAssetLoader<Aseprite, AsepriteParameter>(resolver) {

    private var data: Aseprite? = null

    override fun loadSync(manager: AssetManager, fileName: String, file: FileHandle,
                          parameter: AsepriteParameter?): Aseprite? {
        val copy = data
        data = null
        return copy
    }

    override fun getDependencies(fileName: String?, file: FileHandle?,
                                 parameter: AsepriteParameter?): Array<AssetDescriptor<out Any>>? {
        val result = Array<AssetDescriptor<out Any>>(2)
        result.add(AssetDescriptor(file?.pathWithoutExtension() + ".json", AsepriteJson::class.java))
        result.add(AssetDescriptor(file?.pathWithoutExtension() + ".png", Texture::class.java))
        return result
    }

    override fun loadAsync(manager: AssetManager, fileName: String, file: FileHandle, parameter: AsepriteParameter?) {
        val texture: Texture = manager[file.pathWithoutExtension() + ".png"]
        val json: AsepriteJson = manager[file.pathWithoutExtension() + ".json"]
        data = Aseprite(texture, json)
    }

}
