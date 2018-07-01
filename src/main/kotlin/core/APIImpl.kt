package therealfarfetchd.quacklib.core

import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.util.ResourceLocation
import therealfarfetchd.quacklib.api.block.BlockReference
import therealfarfetchd.quacklib.api.block.component.*
import therealfarfetchd.quacklib.api.block.data.BlockDataPart
import therealfarfetchd.quacklib.api.block.data.BlockDataRO
import therealfarfetchd.quacklib.api.block.data.DataPartSerializationRegistry
import therealfarfetchd.quacklib.api.core.Unsafe
import therealfarfetchd.quacklib.api.core.modinterface.QuackLibAPI
import therealfarfetchd.quacklib.api.item.ItemReference
import therealfarfetchd.quacklib.api.tools.Logger
import therealfarfetchd.quacklib.api.tools.isDebugMode
import therealfarfetchd.quacklib.block.BlockReferenceByRL
import therealfarfetchd.quacklib.block.BlockReferenceDirect
import therealfarfetchd.quacklib.block.component.ExportedValueImpl
import therealfarfetchd.quacklib.block.component.ImportedValueImpl
import therealfarfetchd.quacklib.block.data.DataPartSerializationRegistryImpl
import therealfarfetchd.quacklib.block.data.ValuePropertiesImpl
import therealfarfetchd.quacklib.block.data.get
import therealfarfetchd.quacklib.block.data.set
import therealfarfetchd.quacklib.block.multipart.MultipartAPIInternal
import therealfarfetchd.quacklib.item.ItemReferenceByRL
import therealfarfetchd.quacklib.item.ItemReferenceDirect
import therealfarfetchd.quacklib.tools.ModContext
import therealfarfetchd.quacklib.tools.getResourceFromName
import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import therealfarfetchd.quacklib.block.multipart.cbmp.MultipartAPIImpl as CBMPAPI
import therealfarfetchd.quacklib.block.multipart.mcmp.MultipartAPIImpl as MCMPAPI

object APIImpl : QuackLibAPI {
  override val modContext = ModContext

  override val serializationRegistry: DataPartSerializationRegistry = DataPartSerializationRegistryImpl

  override val multipartAPI: MultipartAPIInternal = MCMPAPI

  override var qlVersion: String = "unset"

  override fun getItem(name: String): ItemReference =
    ItemReferenceByRL(getResourceFromName(name))

  override fun getItem(item: Item): ItemReference =
    ItemReferenceDirect(item)

  override fun getItem(rl: ResourceLocation): ItemReference =
    ItemReferenceByRL(rl)

  override fun getBlock(name: String): BlockReference =
    BlockReferenceByRL(getResourceFromName(name))

  override fun getBlock(block: Block): BlockReference =
    BlockReferenceDirect(block)

  override fun getBlock(rl: ResourceLocation): BlockReference =
    BlockReferenceByRL(rl)

  override fun addItemToBlock(configurationScope: BlockConfiguration, name: String, op: ItemConfigurationScope.() -> Unit) {
    configurationScope as BlockConfigurationScopeImpl

    configurationScope.init.addItem(name) {
      if (configurationScope.isMultipart) apply(multipartAPI.createPlacementComponent(configurationScope))
      else apply(ComponentPlaceBlock(block(configurationScope.rl)))
      op(this)
    }

    configurationScope.apply(ComponentItemForBlock(item(name)))
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T> createBlockDataDelegate(part: BlockDataPart, name: String, type: KClass<*>, default: T, persistent: Boolean, sync: Boolean, render: Boolean, validValues: List<T>?): ReadWriteProperty<BlockDataPart, T> {
    val delegate = object : ReadWriteProperty<BlockDataPart, T> {

      @Suppress("UNCHECKED_CAST")
      override fun getValue(thisRef: BlockDataPart, property: KProperty<*>): T {
        return part.storage.get(name) as T
      }

      override fun setValue(thisRef: BlockDataPart, property: KProperty<*>, value: T) {
        part.storage.set(name, value)
      }

    }

    if (name in part.defs) error("Duplicate name")

    part.addDefinition(name, ValuePropertiesImpl(name, type as KClass<Any>, default, persistent, sync, render, validValues))

    return delegate
  }

  override fun <T, C : BlockComponentDataImport<C, D>, D : ImportedData<D, C>> createImportedValue(target: C): ImportedValue<T> {
    return ImportedValueImpl()
  }

  override fun <R, C : BlockComponentDataExport<C, D>, D : ExportedData<D, C>> createExportedValue(target: C, op: (C, BlockDataRO) -> R): ExportedValue<D, R> {
    return ExportedValueImpl { data -> op(target, data) }
  }

  override fun <T : Any> registerCapability(type: KClass<T>) {
    therealfarfetchd.quacklib.tools.registerCapability(type)
  }

  override fun openResource(rl: ResourceLocation, respectResourcePack: Boolean): InputStream? {
    return QuackLib.proxy.openResource(rl, respectResourcePack)
  }

  override fun <R> unsafeOps(op: (Unsafe) -> R): R {
    return op(UnsafeImpl)
  }

  override fun logException(e: Throwable) {
    Logger.error(e)
    val sw = StringWriter()
    val pw = PrintWriter(sw)
    e.printStackTrace(pw)
    sw.toString().lines().forEach {
      if (isDebugMode) Logger.error(it)
      else Logger.debug(it)
    }
  }

}