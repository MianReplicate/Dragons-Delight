package mian.minecraft.dragons_delight;

import by.dragonsurvivalteam.dragonsurvival.common.codecs.DietEntry;
import by.dragonsurvivalteam.dragonsurvival.registry.data_maps.DietEntryCache;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

import java.util.*;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DragonsDelight.MODID)
public class DragonsDelight {
    // dragons cant do cooked stuff
    public static final List<TagKey<Item>> EXCLUDE = List.of(
            Tags.Items.FOODS_COOKED_MEAT,
            Tags.Items.FOODS_COOKED_FISH
    );

    // should not be accounted for recipes
    public static final List<Item> NOT_INCLUDED_IN_INGREDIENTS = List.of(
            Items.BOWL,
            Items.MILK_BUCKET
    );

    // Define mod id in a common place for everything to reference
    public static final String MODID = "dragons_delight";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public DragonsDelight(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Time to delight your dragon with feasts!");
    }

    public static List<Ingredient> getIngredientsFor(MinecraftServer server, Item item){
        Optional<RecipeHolder<?>> recipeRef =
                server.getRecipeManager().getRecipes()
                        .stream().filter(recipe ->
                                recipe.id().equals(BuiltInRegistries.ITEM.getKey(item))).findFirst();
        if(!recipeRef.isPresent())
            return null;

        return recipeRef.get().value().getIngredients();
    }

    public static Optional<DietEntry> makeFoodEntryBasedOnDiet(MinecraftServer server, List<Item> diet, Item item){
        if(item.components().has(DataComponents.FOOD)){
            if(EXCLUDE.stream().anyMatch(tag -> item.getDefaultInstance().is(tag)))
                return Optional.empty();

            FoodProperties foodProperties = item.components().get(DataComponents.FOOD);

            List<Ingredient> ingredients = getIngredientsFor(server, item);
            if(ingredients != null){
                List<Item> itemsInvolved = new ArrayList<>(ingredients.stream()
                        .flatMap(ingredient -> Arrays.stream(ingredient.getItems()))
                        .map(ItemStack::getItem)
                        .toList());
                itemsInvolved.removeAll(DragonsDelight.NOT_INCLUDED_IN_INGREDIENTS);

                int max = itemsInvolved.size();

                if(max == 0)
                    return Optional.empty(); // literally no ingredients involved

                int matching = Math.toIntExact(itemsInvolved.stream().filter(diet::contains).count());

                if(matching == 0)
                    return Optional.empty(); // literally no ingredients match

                int nutrition = Math.round(foodProperties.nutrition() * ((float) matching / max));

                if(nutrition <= 0)
                    return Optional.empty(); // pointless lmfao

                return Optional.of(
                        DietEntry.from(item, new FoodProperties(
                                nutrition,
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


//    @SubscribeEvent
//    public void registerDatapacks(GatherDataEvent event){
//        event.addProvider(new DietEntries(event.getGenerator().getPackOutput(), event.getLookupProvider()));
//    }
//
//    public static class DietEntries extends DataMapProvider{
//        HolderLookup.Provider provider;
//
//        HashMap<ResourceKey<DragonSpecies>, List<Item>> dragonDiets;
//        HashMap<Item, List<Item>> recipes;
//
//        public static final List<Item> exclude = List.of(
//                Items.BOWL,
//                Items.MILK_BUCKET,
//                Items.SUGAR,
//                Items.COCOA_BEANS
//        );
//
//        protected DietEntries(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
//            super(packOutput, lookupProvider);
//        }
//
//        @Override
//        protected void gather(HolderLookup.@NotNull Provider provider) {
//            this.provider = provider;
//            addDragonDiets();
//            addRecipes();
//            makeFoodEntriesForDragons();
//        }
//
//        public void addDragonDiets(){
//            dragonDiets = new HashMap<>();
//
//            dragonDiets.put(BuiltInDragonSpecies.CAVE_DRAGON, List.of());
//
//            dragonDiets.put(BuiltInDragonSpecies.FOREST_DRAGON, List.of());
//            dragonDiets.put(BuiltInDragonSpecies.SEA_DRAGON, List.of());
//            dragonDiets.put(BuiltInDragonSpecies.key(DragonSurvival.res("tundra_dragon")), List.of());
//            dragonDiets.put(BuiltInDragonSpecies.key(DragonSurvival.res("aether_dragon")), List.of());
//        }
//
//        public void addRecipes(){
//            recipes = new HashMap<>();
//        }
//
//        public Optional<DietEntry> makeFoodEntryBasedOnDiet(List<Item> diet, Item item){
//            List<Item> ingredients = recipes.get(item);
//            if(item.components().has(DataComponents.FOOD) && ingredients != null){
//                FoodProperties foodProperties = item.components().get(DataComponents.FOOD);
//
//                ingredients = new ArrayList<>(ingredients);
//                ingredients.removeAll(exclude);
//
//                int max = ingredients.size();
//                int matching = Math.toIntExact(ingredients.stream().filter(diet::contains).count());
//
//                return Optional.of(
//                        DietEntry.from(item, new FoodProperties(
//                                foodProperties.nutrition() * (matching / max),
//                                foodProperties.saturation() * ((float) matching / max),
//                                foodProperties.canAlwaysEat(),
//                                foodProperties.eatSeconds(),
//                                foodProperties.usingConvertsTo(),
//                                foodProperties.effects()
//                        ))
//                );
//            }
//            return Optional.empty();
//        }
//
//        public void makeFoodEntriesForDragons(){
//            Builder<List<DietEntry>, DragonSpecies> builder = builder(DSDataMaps.DIET_ENTRIES);
//            for(Map.Entry<ResourceKey<DragonSpecies>, List<Item>> speciesEntry : dragonDiets.entrySet()) {
//                ArrayList<DietEntry> dietEntries = new ArrayList<>();
//
//                for(Item item : BuiltInRegistries.ITEM){
//                    Optional<DietEntry> optionalEntry = makeFoodEntryBasedOnDiet(speciesEntry.getValue(), item);
//                    optionalEntry.ifPresent(dietEntries::add);
//                }
//
//                builder = builder.add(
//                        speciesEntry.getKey(),
//                        dietEntries,
//                        false,
//                        new RegisteredCondition<>(speciesEntry.getKey()));
//            }
//        }
//
//        @Override
//        public @NotNull String getName() {
//            return "Dragon's Delight Diet Entries";
//        }
//    }
}
