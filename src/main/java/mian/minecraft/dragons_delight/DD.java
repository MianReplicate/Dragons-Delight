package mian.minecraft.dragons_delight;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvival;
import by.dragonsurvivalteam.dragonsurvival.common.codecs.DietEntry;
import by.dragonsurvivalteam.dragonsurvival.registry.DSConditions;
import by.dragonsurvivalteam.dragonsurvival.registry.DSDataMaps;
import by.dragonsurvivalteam.dragonsurvival.registry.data_maps.DietEntryCache;
import by.dragonsurvivalteam.dragonsurvival.registry.datagen.data_maps.RegisteredCondition;
import by.dragonsurvivalteam.dragonsurvival.registry.dragon.BuiltInDragonSpecies;
import by.dragonsurvivalteam.dragonsurvival.registry.dragon.DragonSpecies;
import by.dragonsurvivalteam.dragonsurvival.util.ResourceHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import vectorwing.farmersdelight.FarmersDelight;
import vectorwing.farmersdelight.common.FoodValues;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DD.MODID)
public class DD {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "dragons_delight";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public DD(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Time to delight your dragon with feasts!");
        modEventBus.register(this);
    }

    @SubscribeEvent
    public void registerDatapacks(GatherDataEvent event){
        event.addProvider(new DietEntries(event.getGenerator().getPackOutput(), event.getLookupProvider()));
    }

    public static class DietEntries extends DataMapProvider{
//        RecipeManager manager;
        HolderLookup.Provider provider;
        List<Holder.Reference<DragonSpecies>> dragons;
        List<Holder.Reference<Recipe<?>>> recipes;

        public static final List<Item> exclude = List.of(
                Items.BOWL
        );

        protected DietEntries(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(packOutput, lookupProvider);

//            manager = ServerLifecycleHooks.getCurrentServer().getRecipeManager();
        }

        @Override
        protected void gather(HolderLookup.@NotNull Provider provider) {
            this.provider = provider;
            dragons = provider.lookupOrThrow(DragonSpecies.REGISTRY).listElements().toList();
            recipes = provider.lookupOrThrow(Registries.RECIPE).listElements().toList();

            makeFoodEntriesForDragons(provider);
        }

        public List<Ingredient> getIngredientsFor(Item item){
            // should check for namespace & location
//            RecipeHolder holder = manager.getRecipes().stream().filter(recipe -> recipe.id()
//                    .equals(BuiltInRegistries.ITEM.getKey(item))).findFirst().orElseGet(null);
//            if(holder == null)
//                return null;
//
//            return holder.value().getIngredients();
            Optional<Holder.Reference<Recipe<?>>> recipeRef =
                    recipes.stream().filter(recipe ->
                            recipe.is(BuiltInRegistries.ITEM.getKey(item))).findFirst();
            if(!recipeRef.isPresent())
                return null;

            return recipeRef.get().value().getIngredients();
        }

        public Optional<DietEntry> makeFoodEntryBasedOnDiet(List<Item> diet, Item item){
            if(item.components().has(DataComponents.FOOD)){
                FoodProperties foodProperties = item.components().get(DataComponents.FOOD);

                List<Ingredient> ingredients = getIngredientsFor(item);
                if(ingredients != null){
                    List<Item> itemsInvolved = new ArrayList<>(ingredients.stream()
                            .flatMap(ingredient -> Arrays.stream(ingredient.getItems()))
                            .map(ItemStack::getItem)
                            .toList());
                    itemsInvolved.removeAll(exclude);

                    int max = itemsInvolved.size();
                    int matching = Math.toIntExact(itemsInvolved.stream().filter(diet::contains).count());

                    return Optional.of(
                            DietEntry.from(item, new FoodProperties(
                                    foodProperties.nutrition() * (matching / max),
                                    foodProperties.saturation() * ((float) matching / max),
                                    foodProperties.canAlwaysEat(),
                                    foodProperties.eatSeconds(),
                                    foodProperties.usingConvertsTo(),
                                    foodProperties.effects()
                            ))
                    );
                }
            }
            return Optional.empty();
        }

        public void makeFoodEntriesForDragons(HolderLookup.Provider provider){
            Builder<List<DietEntry>, DragonSpecies> builder = builder(DSDataMaps.DIET_ENTRIES);
            for(Holder.Reference<DragonSpecies> species : ResourceHelper.all(provider, DragonSpecies.REGISTRY)) {
                List<Item> diet = DietEntryCache.getDietItems(species);
                ArrayList<DietEntry> dietEntries = new ArrayList<>();

                for(Item item : BuiltInRegistries.ITEM){
                    Optional<DietEntry> optionalEntry = makeFoodEntryBasedOnDiet(diet, item);
                    optionalEntry.ifPresent(dietEntries::add);
                }

                builder = builder.add(
                        species.getKey(),
                        dietEntries,
                        false,
                        new RegisteredCondition<>(species.getKey()));
            }
        }

        @Override
        public @NotNull String getName() {
            return "Dragon's Delight Diet Entries";
        }
    }
}
