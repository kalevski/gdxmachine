package com.disgraded.gdxmachine.framework.drawables

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.disgraded.gdxmachine.framework.core.graphics.utils.Color
import com.disgraded.gdxmachine.framework.utils.Corner
import com.disgraded.gdxmachine.framework.core.Prototype

class Mask : Drawable2D(), Prototype<Mask> {

    private var textureRegionMask: TextureRegion? = null
    private var textureRegion: TextureRegion? = null

    private val colorMap = hashMapOf<Corner, Color>()

    init {
        colorMap[Corner.TOP_LEFT] = Color.WHITE
        colorMap[Corner.TOP_RIGHT] = Color.WHITE
        colorMap[Corner.BOTTOM_LEFT] = Color.WHITE
        colorMap[Corner.BOTTOM_RIGHT] = Color.WHITE
    }

    fun setTexture(texture: Texture) {
        setTexture(TextureRegion(texture))
    }

    fun setTexture(textureRegion: TextureRegion) {
        this.textureRegion = textureRegion
    }

    fun getTexture(): TextureRegion {
        if (textureRegion == null) throw RuntimeException("Texture doesn't exist!")
        return textureRegion!!
    }

    fun setColor(color: Color) {
        colorMap[Corner.TOP_LEFT] = color
        colorMap[Corner.TOP_RIGHT] = color
        colorMap[Corner.BOTTOM_LEFT] = color
        colorMap[Corner.BOTTOM_RIGHT] = color
    }

    fun setColor(corner: Corner, color: Color) {
        when(corner) {
            Corner.TOP_LEFT -> colorMap[Corner.TOP_LEFT] = color
            Corner.TOP_RIGHT -> colorMap[Corner.TOP_RIGHT] = color
            Corner.BOTTOM_LEFT -> colorMap[Corner.BOTTOM_LEFT] = color
            Corner.BOTTOM_RIGHT -> colorMap[Corner.BOTTOM_RIGHT] = color
        }
    }

    fun getColor(): Color {
        return colorMap[Corner.TOP_LEFT]!!
    }

    fun getColor(corner: Corner): Color {
        return when(corner) {
            Corner.TOP_LEFT -> colorMap[Corner.TOP_LEFT]!!
            Corner.TOP_RIGHT -> colorMap[Corner.TOP_RIGHT]!!
            Corner.BOTTOM_LEFT -> colorMap[Corner.BOTTOM_LEFT]!!
            Corner.BOTTOM_RIGHT -> colorMap[Corner.BOTTOM_RIGHT]!!
        }
    }


    fun setMask(texture: Texture) {
        setMask(TextureRegion(texture))
    }

    fun setMask(textureRegion: TextureRegion) {
        this.textureRegionMask = textureRegion
    }

    fun getMask(): TextureRegion {
        if (textureRegionMask == null) throw RuntimeException("Mask texture doesn't exist!")
        return textureRegionMask!!
    }

    override fun copy(): Mask {
        val mask = Mask()
        mask.inherit(this)
        return mask
    }

    override fun inherit(obj: Mask) {
        setTexture(obj.getTexture())
        setMask(obj.getMask())
        setColor(obj.getColor(Corner.TOP_LEFT))
        setColor(obj.getColor(Corner.TOP_RIGHT))
        setColor(obj.getColor(Corner.BOTTOM_LEFT))
        setColor(obj.getColor(Corner.BOTTOM_RIGHT))
        super.inherit(obj)
    }
}