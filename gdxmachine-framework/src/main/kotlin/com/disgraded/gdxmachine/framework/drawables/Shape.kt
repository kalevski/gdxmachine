package com.disgraded.gdxmachine.framework.drawables
import com.badlogic.gdx.math.Shape2D
import com.disgraded.gdxmachine.framework.core.Prototype
import com.disgraded.gdxmachine.framework.core.graphics.utils.Color
import kotlin.reflect.KClass

abstract class Shape : Drawable2D() {
    enum class Style {
        FILLED, LINE, POINT
    }

    var style = Style.FILLED
    var color = Color.WHITE

    abstract fun getType(): KClass<out Shape2D>

    abstract fun getShape2D(): Shape2D
}