package com.disgraded.gdxmachine.core.api.graphics.renderer

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.disgraded.gdxmachine.core.api.graphics.ShaderFactory
import com.disgraded.gdxmachine.core.api.graphics.drawable.Drawable2D
import com.disgraded.gdxmachine.core.api.graphics.drawable.Sprite




class SpriteRenderer : Renderer<Sprite> {

    companion object {
        private const val MAX_BUFFERED_CALLS = 1500
        private const val BUFFER_SIZE = 24
        private const val VERTICES_PER_BUFFER = 4
        private const val INDICES_PER_BUFFER = 6
    }

    private val shaderFactory = ShaderFactory.getInstance()

    private val mesh: Mesh
    private val vertices: FloatArray
    private val indices: ShortArray
    private val shaderProgram: ShaderProgram
    private lateinit var projectionMatrix: Matrix4

    private var active = false
    private var gpuCalls = 0

    private var bufferedCalls = 0
    private var cachedTexture: Texture? = null

    init {
        val maxVertices = VERTICES_PER_BUFFER * MAX_BUFFERED_CALLS
        val maxIndices = INDICES_PER_BUFFER * MAX_BUFFERED_CALLS
        val vertexAttributes = VertexAttributes(
                VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                VertexAttribute(Usage.ColorPacked, 4, ShaderProgram.COLOR_ATTRIBUTE),
                VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE)
        )

        mesh = Mesh(false, maxVertices, maxIndices, vertexAttributes)

        vertices = FloatArray(MAX_BUFFERED_CALLS * BUFFER_SIZE)
        indices = ShortArray(MAX_BUFFERED_CALLS * INDICES_PER_BUFFER)

        // generate indices
        for (i in indices.indices) {
            val module = i % 6
            val idx = (i / 6) * 4
            if (module == 0 || module == 5) indices[i] = idx.toShort()
            if (module == 1) indices[i] = (idx + 1).toShort()
            if (module == 2 || module == 3) indices[i] = (idx + 2).toShort()
            if (module == 4) indices[i] = (idx + 3).toShort()
        }

        shaderProgram = shaderFactory.get("sprite", "sprite")
    }

    override fun begin() {
        if(active) throw RuntimeException("The renderer is already active")
        gpuCalls = 0
        shaderProgram.begin()
        active = true
    }

    override fun draw(drawable: Sprite) {
        if(drawable.textureRegion == null) throw RuntimeException("Sprite can not be rendered without texture")
        validateCachedTexture(drawable.textureRegion!!.texture)
        validateShader(drawable)
        if (bufferedCalls == MAX_BUFFERED_CALLS) {
            flush()
        }
        appendVertices(drawable)
        bufferedCalls++
    }

    override fun end() {
        if(!active) throw RuntimeException("The renderer isn't active")
        if (bufferedCalls > 0) flush()
        cachedTexture = null
        active = false
        shaderProgram.end()
        gpuCalls = 0
    }

    override fun setProjectionMatrix(projectionMatrix: Matrix4) {
        if (active) {
            flush()
        }
        this.projectionMatrix = projectionMatrix
    }

    override fun dispose() = mesh.dispose()

    private fun appendVertices(sprite: Sprite) {
        val idx = bufferedCalls * BUFFER_SIZE
        val sizeX = sprite.textureRegion!!.regionWidth * sprite.scaleX
        val sizeY = sprite.textureRegion!!.regionHeight * sprite.scaleY

        var x1 = sprite.x - (sizeX * sprite.anchorX)
        var y1 = sprite.y - (sizeY * sprite.anchorY)
        var x2 = x1
        var y2 = y1 + sizeY
        var x3 = x1 + sizeX
        var y3 = y1 + sizeY
        var x4 = x1 + sizeX
        var y4 = y1

        sprite.rotation %= 360f
        if (sprite.rotation != 0f) {
            val cos = MathUtils.cosDeg(sprite.rotation)
            val sin = MathUtils.sinDeg(sprite.rotation)
            val rx1 = cos * x1 - sin * y1
            val ry1 = sin * x1 + cos * y1
            val rx2 = cos * x2 - sin * y2
            val ry2 = sin * x2 + cos * y2
            val rx3 = cos * x3 - sin * y3
            val ry3 = sin * x3 + cos * y3
            x1 = rx1
            y1 = ry1
            x2 = rx2
            y2 = ry2
            x3 = rx3
            y3 = ry3
            x4 = x1 + (x3 - x2)
            y4 = y3 - (y2 - y1)
        }

        vertices[idx] = x1
        vertices[idx + 1] = y1
        vertices[idx + 2] = sprite.z
        vertices[idx + 3] = sprite.getColor(Drawable2D.Corner.BOTTOM_LEFT).toFloatBits()
        vertices[idx + 4] = sprite.textureRegion!!.u
        vertices[idx + 5] = sprite.textureRegion!!.v2

        vertices[idx + 6] = x2
        vertices[idx + 7] = y2
        vertices[idx + 8] = sprite.z
        vertices[idx + 9] = sprite.getColor(Drawable2D.Corner.TOP_LEFT).toFloatBits()
        vertices[idx + 10] = sprite.textureRegion!!.u
        vertices[idx + 11] = sprite.textureRegion!!.v

        vertices[idx + 12] = x3
        vertices[idx + 13] = y3
        vertices[idx + 14] = sprite.z
        vertices[idx + 15] = sprite.getColor(Drawable2D.Corner.TOP_RIGHT).toFloatBits()
        vertices[idx + 16] = sprite.textureRegion!!.u2
        vertices[idx + 17] = sprite.textureRegion!!.v

        vertices[idx + 18] = x4
        vertices[idx + 19] = y4
        vertices[idx + 20] = sprite.z
        vertices[idx + 21] = sprite.getColor(Drawable2D.Corner.BOTTOM_RIGHT).toFloatBits()
        vertices[idx + 22] = sprite.textureRegion!!.u2
        vertices[idx + 23] = sprite.textureRegion!!.v2
    }

    private fun flush() {
        if(bufferedCalls == 0) return

        gpuCalls++
        val indicesCount = bufferedCalls * INDICES_PER_BUFFER
        val verticesCount = bufferedCalls * BUFFER_SIZE

        cachedTexture!!.bind()
        shaderProgram.setUniformMatrix("u_projectionTrans", projectionMatrix);
        shaderProgram.setUniformi("u_texture", 0);
        mesh.setVertices(vertices, 0, verticesCount)
        mesh.setIndices(indices, 0, indicesCount)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFuncSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        mesh.render(shaderProgram, GL20.GL_TRIANGLES, 0, indicesCount)
        bufferedCalls = 0
    }

    private fun validateCachedTexture(texture: Texture) {
        if (cachedTexture !== texture) {
            if (bufferedCalls > 0) flush()
            cachedTexture = texture
        }
    }

    private fun validateShader(sprite: Sprite) {

    }
}