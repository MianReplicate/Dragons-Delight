package mian.minecraft.dragons_delight;

import by.dragonsurvivalteam.dragonsurvival.common.codecs.DietEntry;
import by.dragonsurvivalteam.dragonsurvival.registry.DSConditions;
import by.dragonsurvivalteam.dragonsurvival.registry.DSDataMaps;
import by.dragonsurvivalteam.dragonsurvival.registry.dragon.BuiltInDragonSpecies;
import by.dragonsurvivalteam.dragonsurvival.registry.dragon.DragonSpecies;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;

// TODO: maybe do some datagen for some tags and items?

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DragonsDelight.MODID)
public class DragonsDelight {
    // cooked stuff should be excluded from automatic inclusion
    // add support for diet entry remover, use datagen to exclude cooked meat for default dragons
    public static final List<TagKey<Item>> COOKED_STUFF = List.of(
            Tags.Items.FOODS_COOKED_MEAT,
            Tags.Items.FOODS_COOKED_FISH,
            tag("foods/cooked_egg"),
            tag("foods/cooked_bacon")
    );

    // should not be accounted for recipes, maybe convert to configuration?
    public static final List<Item> NOT_INCLUDED_IN_INGREDIENTS = List.of(
            Items.BOWL,
            Items.MILK_BUCKET
    );

    public static final String MODID = "dragons_delight";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DragonsDelight(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Time to delight your dragon with feasts upon feats!");
        modEventBus.register(this);
    }

    public static List<Ingredient> getIngredientsFor(MinecraftServer server, Item item){
        Optional<RecipeHolder<?>> recipeRef =
                server.getRecipeManager().getRecipes()
                        .stream().filter(recipe ->
                                recipe.value().getResultItem(server.registryAccess()).is(item)).findFirst();
        if(!recipeRef.isPresent())
            return null;

        return recipeRef.get().value().getIngredients();
    }

    public static Optional<DietEntry> makeFoodEntryBasedOnDiet(MinecraftServer server, List<Item> diet, Item item){
        if(item.components().has(DataComponents.FOOD)){

            // remove later on when we figure out how to use datagen to exclude tags
            if(COOKED_STUFF.stream().anyMatch(tag -> item.getDefaultInstance().is(tag)))
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

                int nutrition = (int) (foodProperties.nutrition() * ((float) matching / max));
                float saturation = foodProperties.saturation() * ((float) matching / max);

                boolean noNutrition = nutrition == 0;
                boolean noSaturation = saturation == 0;

                for(Item ingredient : itemsInvolved.stream().filter(diet::contains).toList()){
                    FoodProperties properties = ingredient.components().get(DataComponents.FOOD);
                    if(properties != null){
                        if(noNutrition)
                            nutrition += properties.nutrition();
                        if(noSaturation)
                            saturation += properties.saturation();
                    }
                }

                if(nutrition <= 0)
                    return Optional.empty(); // pointless

                return Optional.of(
                        DietEntry.from(item, new FoodProperties(
                                nutrition,
                                saturation,
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

    public static TagKey<Item> tag(String name) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", name));
    }

    @SubscribeEvent
    public void registerDatapacks(GatherDataEvent event){
        event.addProvider(new DietEntries(event.getGenerator().getPackOutput(), event.getLookupProvider()));
    }

    public static class DietEntries extends DataMapProvider {
        protected DietEntries(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(packOutput, lookupProvider);
        }

        @Override
        protected void gather(HolderLookup.@NotNull Provider provider) {
            Builder<List<DietEntry>, DragonSpecies> builder = builder(DSDataMaps.DIET_ENTRIES);
            builder.add(
                    BuiltInDragonSpecies.CAVE_DRAGON,
                    COOKED_STUFF.stream().map(DietEntry::from).toList(),
                    false,
                    DSConditions.CAVE_DRAGON_LOADED
            ).build();

            builder.add(
                    BuiltInDragonSpecies.FOREST_DRAGON,
                    COOKED_STUFF.stream().map(DietEntry::from).toList(),
                    false,
                    DSConditions.FOREST_DRAGON_LOADED
            ).build();

            builder.add(
                    BuiltInDragonSpecies.SEA_DRAGON,
                    COOKED_STUFF.stream().map(DietEntry::from).toList(),
                    false,
                    DSConditions.SEA_DRAGON_LOADED
            ).build();
        }

        @Override
        public @NotNull String getName() {
            return "Dragon's Delight Diet Entries";
        }
    }
}
