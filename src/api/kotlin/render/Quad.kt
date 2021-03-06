package therealfarfetchd.quacklib.api.render

import net.minecraft.util.EnumFacing
import therealfarfetchd.math.Mat4
import therealfarfetchd.math.Vec2
import therealfarfetchd.math.Vec3
import therealfarfetchd.quacklib.api.core.extensions.toVec3
import therealfarfetchd.quacklib.api.render.texture.AtlasTexture
import therealfarfetchd.quacklib.api.tools.Axis
import therealfarfetchd.quacklib.api.tools.Facing
import java.awt.Color

typealias Quad = QuadBase<AtlasTexture>

data class QuadBase<T>(
  val texture: T,
  val vert1: Vec3, val vert2: Vec3, val vert3: Vec3, val vert4: Vec3,
  val tex1: Vec2, val tex2: Vec2, val tex3: Vec2, val tex4: Vec2,
  val lightmap: Vec2,
  val color: Color
) {
  val normal by lazy { ((vert2 - vert1) crossProduct (vert3 - vert1)).normalized }

  val facing: Facing by lazy { Facing.getFacingFromVector(normal.x, normal.y, normal.z) }

  fun xyzComponents(): List<Vec3> = listOf(vert1, vert2, vert3, vert4)

  /**
   * Translates (offsets) this quad by the given vector
   *
   * @return The moved quad
   */

  fun translate(vec: Vec3) =
    transform(Mat4.translate(vec.x, vec.y, vec.z))

  /**
   * Rotates the quad around the given axis with the angle a
   *
   * @param axis The axis to rotate around
   * @param a    The angle (0..360)
   * @return The rotated quad
   */

  fun rotate(axis: Axis, a: Float, center: Vec3 = Vec3(0.5f, 0.5f, 0.5f)): QuadBase<T> {
    return if (a == 0f) this.copy()
    else {
      val rotate = rotateAround(a, axis, center)
      val r = listOf(vert1, vert2, vert3, vert4).map { rotateAround(a, axis, center) * it }
      QuadBase(texture, r[0], r[1], r[2], r[3], tex1, tex2, tex3, tex4, lightmap, color)
    }
  }

  private fun rotateAround(angle: Float, axis: Axis, center: Vec3): Mat4 {
    val x = if (axis == Axis.X) 1f else 0f
    val y = if (axis == Axis.Y) 1f else 0f
    val z = if (axis == Axis.Z) 1f else 0f

    return Mat4.translate(center)
      .rotate(x, y, z, angle)
      .translate(-center)
  }

  fun transform(mat: Mat4) =
    copy(vert1 = mat * vert1, vert2 = mat * vert2, vert3 = mat * vert3, vert4 = mat * vert4)

  fun lightmap(x: Float, y: Float) =
    copy(lightmap = Vec2(x, y))

  fun rotateTexture(angle: Int): QuadBase<T> {
    require(angle % 90 == 0) { "Angle must be a multiple of 90°!" }
    val rotate = Math.floorMod(angle / 90, 4)
    if (rotate == 0) return this
    val (v1, v2, v3, v4) = listOf(tex1, tex2, tex3, tex4).let { it + it }.subList(4 - rotate, 8 - rotate)
    return copy(tex1 = v1, tex2 = v2, tex3 = v3, tex4 = v4)
  }

  /**
   * Flips the texture on the x axis.
   */
  val mirrorTextureX by lazy {
    copy(
      tex1 = Vec2(1 - tex1.x, tex1.y),
      tex2 = Vec2(1 - tex2.x, tex2.y),
      tex3 = Vec2(1 - tex3.x, tex3.y),
      tex4 = Vec2(1 - tex4.x, tex4.y))
  }

  /**
   * Flips the texture on the y axis.
   */
  val mirrorTextureY by lazy {
    copy(
      tex1 = Vec2(tex1.x, 1 - tex1.y),
      tex2 = Vec2(tex2.x, 1 - tex2.y),
      tex3 = Vec2(tex3.x, 1 - tex3.y),
      tex4 = Vec2(tex4.x, 1 - tex4.y))
  }

  /**
   * Flips the textured side of this quad.
   */
  val flipTexturedSide by lazy { copy(vert1 = vert2, vert2 = vert1, vert3 = vert4, vert4 = vert3).mirrorTextureY }

  fun mirror(axis: Axis): QuadBase<T> {
    // TODO this might not work
    val x = Vec3(1, 1, 1) - (EnumFacing.getFacingFromAxis(EnumFacing.AxisDirection.POSITIVE, axis).directionVec.toVec3() * 2)
    return transform(Mat4.scale(x.x, x.y, x.z))
  }

  fun <R> withTexture(texture: R): QuadBase<R> =
    QuadBase(texture, vert1, vert2, vert3, vert4, tex1, tex2, tex3, tex4, lightmap, color)
}