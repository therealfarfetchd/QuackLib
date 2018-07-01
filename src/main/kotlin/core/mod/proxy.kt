package therealfarfetchd.quacklib.core.mod

import net.minecraft.block.Block
import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.item.Item
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.event.RegistryEvent
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.EntityEntry
import therealfarfetchd.quacklib.api.block.init.BlockConfiguration
import therealfarfetchd.quacklib.api.core.mod.BaseMod
import therealfarfetchd.quacklib.api.core.mod.ModProxy
import therealfarfetchd.quacklib.api.core.modinterface.item
import therealfarfetchd.quacklib.api.tools.Logger
import therealfarfetchd.quacklib.block.impl.BlockQuackLib
import therealfarfetchd.quacklib.block.init.BlockConfigurationScopeImpl
import therealfarfetchd.quacklib.core.APIImpl
import therealfarfetchd.quacklib.core.init.InitializationContextImpl
import therealfarfetchd.quacklib.core.init.ItemConfigurationScopeImpl
import therealfarfetchd.quacklib.core.init.TabConfigurationScopeImpl
import therealfarfetchd.quacklib.item.impl.ItemQuackLib
import therealfarfetchd.quacklib.item.impl.TabQuackLib

sealed class CommonProxy : ModProxy {

  override lateinit var mod: BaseMod

  override var customProxy: Any? = null

  protected var blockTemplates: Set<BlockConfigurationScopeImpl> = emptySet()
  protected var itemTemplates: Set<ItemConfigurationScopeImpl> = emptySet()
  protected var tabTemplates: Set<TabConfigurationScopeImpl> = emptySet()

  var creativeTabs: List<TabQuackLib> = emptyList()

  var failThings: List<String> = emptyList()

  override fun preInit(e: FMLPreInitializationEvent) {
    mod.initContent(InitializationContextImpl(mod))
    tabTemplates.forEach {
      creativeTabs += TabQuackLib(it)
    }
  }

  override fun init(e: FMLInitializationEvent) {

  }

  override fun postInit(e: FMLPostInitializationEvent) {
    blockTemplates.filterNot { it.validate() }.forEach { fatalInit(it.describe()) }
    itemTemplates.filterNot { it.validate() }.forEach { fatalInit(it.describe()) }
    tabTemplates.filterNot { it.validate() }.forEach { fatalInit(it.describe()) }

    if (failThings.isNotEmpty()) {
      error("Failed to add ${failThings.size} objects for mod '${mod.modid}'. Look in the message log for more information.")
    }
  }

  @SubscribeEvent
  fun registerBlocks(e: RegistryEvent.Register<Block>) {
    blockTemplates.forEach {
      Logger.info("Adding ${it.describe()}")
      if (it.isMultipart) APIImpl.multipartAPI.registerBlock(e, it)
      else registerBlock(e, it)
    }
  }

  private fun registerBlock(e: RegistryEvent.Register<Block>, def: BlockConfiguration) {
    val block = BlockQuackLib(def)
    e.registry.register(block)
  }

  @SubscribeEvent
  fun registerItems(e: RegistryEvent.Register<Item>) {
    itemTemplates.forEach {
      Logger.info("Adding ${it.describe()}")
      val item = ItemQuackLib(it)
      e.registry.register(item)
    }
  }

  @SubscribeEvent
  fun registerEntities(e: RegistryEvent.Register<EntityEntry>) {
    // TODO
  }

  fun addBlockTemplate(bc: BlockConfigurationScopeImpl) {
    blockTemplates += bc
  }

  fun addItemTemplate(ic: ItemConfigurationScopeImpl) {
    itemTemplates += ic
  }

  fun addTabTemplate(tc: TabConfigurationScopeImpl) {
    tabTemplates += tc
  }

  private fun fatalInit(text: String) {
    failThings += text
  }

}

class ClientProxy : CommonProxy() {

  @SubscribeEvent
  fun registerModels(e: ModelRegistryEvent) {
    itemTemplates.forEach {
      ModelLoader.setCustomModelResourceLocation(item(it.rl).mcItem, 0, ModelResourceLocation(it.rl, "inventory"))
    }
  }

}

class ServerProxy : CommonProxy()