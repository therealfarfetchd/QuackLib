package therealfarfetchd.quacklib.api.render.property

import net.minecraft.block.properties.IProperty
import net.minecraftforge.common.property.IUnlistedProperty
import therealfarfetchd.quacklib.api.core.Unsafe
import therealfarfetchd.quacklib.api.objects.block.Block

interface RenderProperty<out T> {

  val name: String

  fun getValue(b: Block): T

  fun Unsafe.getMCProperty(): PropertyType<T>

}

sealed class PropertyType<out T> {
  class Standard<T>(val prop: IProperty<*>) : PropertyType<T>()

  class Extended<T>(val prop: IUnlistedProperty<T>) : PropertyType<T>()
}

interface UnsafeExtRP : Unsafe {

  fun <T> RenderProperty<T>.getMCProperty() =
    self.getMCProperty()

}