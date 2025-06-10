package mian.minecraft.dragons_delight;

import by.dragonsurvivalteam.dragonsurvival.DragonSurvival;
import by.dragonsurvivalteam.dragonsurvival.common.codecs.DietEntry;
import by.dragonsurvivalteam.dragonsurvival.registry.DSConditions;
import by.dragonsurvivalteam.dragonsurvival.registry.DSDataMaps;
import by.dragonsurvivalteam.dragonsurvival.registry.datagen.data_maps.RegisteredCondition;
import by.dragonsurvivalteam.dragonsurvival.registry.dragon.BuiltInDragonSpecies;
import by.dragonsurvivalteam.dragonsurvival.registry.dragon.DragonSpecies;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import vectorwing.farmersdelight.FarmersDelight;
import vectorwing.farmersdelight.common.FoodValues;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
        protected DietEntries(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(packOutput, lookupProvider);
        }

        @Override
        protected void gather(HolderLookup.Provider provider) {
            ResourceKey<DragonSpecies> TUNDRA = BuiltInDragonSpecies.key(DragonSurvival.res("tundra_dragon"));
            ResourceKey<DragonSpecies> AETHER = BuiltInDragonSpecies.key(DragonSurvival.res("aether_dragon"));

            builder(DSDataMaps.DIET_ENTRIES)
                    .add(BuiltInDragonSpecies.CAVE_DRAGON, caveDiet(), false, DSConditions.CAVE_DRAGON_LOADED)
                    .add(BuiltInDragonSpecies.FOREST_DRAGON, forestDiet(), false, DSConditions.FOREST_DRAGON_LOADED)
                    .add(BuiltInDragonSpecies.SEA_DRAGON, seaDiet(), false, DSConditions.SEA_DRAGON_LOADED)
                    .add(TUNDRA, tundraDiet(), false, new RegisteredCondition<>(TUNDRA))
                    .add(AETHER, aetherDiet(), false, new RegisteredCondition<>(AETHER));
        }

        public static <T extends Item> DietEntry makeEntryForFood(Supplier<T> foodItem, Function<T, FoodProperties> entry){
            return DietEntry.from(foodItem.get(), entry.apply(foodItem.get()));
        }

        public static FoodProperties createFood(FoodProperties foodProperties){
            return foodProperties;
        }

        public static FoodProperties createFood(int nutrition, float saturation, FoodProperties copyFrom){
            return new FoodProperties(
                    nutrition,
                    saturation,
                    copyFrom.canAlwaysEat(),
                    copyFrom.eatSeconds(),
                    copyFrom.usingConvertsTo(),
                    copyFrom.effects()
            );
        }

        public static FoodProperties createFood(int nutrition, float saturation){
            return new FoodProperties(
                    nutrition,
                    saturation,
                    false,
                    DietEntry.DEFAULT_EAT_SECONDS,
                    Optional.empty(), List.of());
        }

        public static FoodProperties createFood(int nutrition, float saturation, ItemStack convertsTo){
            return new FoodProperties(
                    nutrition,
                    saturation,
                    false,
                    DietEntry.DEFAULT_EAT_SECONDS,
                    Optional.of(convertsTo),
                    List.of()
            );
        }

        public static List<DietEntry> caveDiet(){
            return List.of(
                    makeEntryForFood(ModItems.BARBECUE_STICK, item ->
                            createFood(
                                    FoodValues.BARBECUE_STICK.nutrition() / 3,
                                    FoodValues.BARBECUE_STICK.saturation() / 3)),
                    makeEntryForFood(ModItems.CHOCOLATE_PIE_SLICE, item ->
                            createFood(FoodValues.PIE_SLICE)),
                    makeEntryForFood(ModItems.STUFFED_POTATO, item -> {
                        FoodProperties stuffedPotato = FoodValues.STUFFED_POTATO;
                        return createFood(stuffedPotato.nutrition() / 3, stuffedPotato.saturation() / 3, stuffedPotato);
                    })
            );
        }

        public static List<DietEntry> forestDiet(){
            return List.of(
                    makeEntryForFood(ModItems.SWEET_BERRY_CHEESECAKE_SLICE, item ->
                            createFood(FoodValues.PIE_SLICE)),
                    makeEntryForFood(ModItems.CAKE_SLICE, item ->
                            createFood(FoodValues.CAKE_SLICE)),
                    makeEntryForFood(ModItems.CHOCOLATE_PIE_SLICE, item ->
                            createFood(FoodValues.PIE_SLICE)),
                    makeEntryForFood(ModItems.SWEET_BERRY_COOKIE, item ->
                            createFood(FoodValues.COOKIES)),
                    makeEntryForFood(ModItems.HONEY_COOKIE, item ->
                            createFood(FoodValues.COOKIES)),
                    makeEntryForFood(ModItems.GLOW_BERRY_CUSTARD, item -> {
                        FoodProperties glowBerry = FoodValues.GLOW_BERRY_CUSTARD;
                        return createFood(glowBerry.nutrition() / 4 * 3, glowBerry.saturation() / 4 * 3, glowBerry);
                    }),
                    makeEntryForFood(ModItems.NETHER_SALAD, item ->
                            createFood(FoodValues.NETHER_SALAD)),
                    makeEntryForFood(ModItems.BONE_BROTH, item ->
                    {
                        FoodProperties boneBroth = FoodValues.BONE_BROTH;
                        return createFood(boneBroth.nutrition() / 2, boneBroth.saturation() / 2, boneBroth);
                    }),
                    makeEntryForFood(ModItems.BEEF_STEW, item -> {
                        FoodProperties beefStew = FoodValues.BEEF_STEW;
                        return createFood(beefStew.nutrition() / 3, beefStew.saturation() / 3, beefStew);
                    }),
                    makeEntryForFood(ModItems.CHICKEN_SOUP, item -> {
                        FoodProperties chickenSoup = FoodValues.CHICKEN_SOUP;
                        return createFood(chickenSoup.nutrition() / 4, chickenSoup.saturation() / 4, chickenSoup);
                    }),
                    makeEntryForFood(ModItems.PUMPKIN_SOUP, item -> {
                        FoodProperties pumpkinSoup = FoodValues.PUMPKIN_SOUP;
                        return createFood(pumpkinSoup.nutrition() / 2, pumpkinSoup.saturation() / 2, pumpkinSoup);
                    }),
                    makeEntryForFood(ModItems.MUSHROOM_RICE, item -> {
                        FoodProperties mushroomRice = FoodValues.MUSHROOM_RICE;
                        return createFood(mushroomRice.nutrition() / 2, mushroomRice.saturation() / 2, mushroomRice);
                    }),
                    makeEntryForFood(ModItems.VEGETABLE_NOODLES, item -> {
                        FoodProperties vegetableNoodles = FoodValues.VEGETABLE_NOODLES;
                        return createFood(vegetableNoodles.nutrition() / 5, vegetableNoodles.saturation() / 5, vegetableNoodles);
                    }),
                    makeEntryForFood(ModItems.STUFFED_PUMPKIN, item -> {
                        FoodProperties stuffedPumpkin = FoodValues.STUFFED_PUMPKIN;
                        return createFood((stuffedPumpkin.nutrition() / 3) * 2, (stuffedPumpkin.saturation() / 3) * 2, stuffedPumpkin);
                    }),
                    makeEntryForFood(ModItems.HONEY_GLAZED_HAM, item -> {
                        FoodProperties honeyHam = FoodValues.HONEY_GLAZED_HAM;
                        return createFood((honeyHam.nutrition() / 8) * 5, (honeyHam.saturation() / 8) * 5, honeyHam);
                    }),
                    makeEntryForFood(ModItems.CABBAGE_ROLLS, item -> {
                        FoodProperties cabbageRoll = FoodValues.CABBAGE_ROLLS;
                        return createFood(cabbageRoll.nutrition() / 2, cabbageRoll.saturation() / 2, cabbageRoll);
                    }),
                    makeEntryForFood(ModItems.NOODLE_SOUP, item -> {
                        FoodProperties noodleSoup = FoodValues.NOODLE_SOUP;
                        return createFood(noodleSoup.nutrition() / 4, noodleSoup.saturation() / 4, noodleSoup);
                    }),
                    makeEntryForFood(ModItems.DUMPLINGS, item -> {
                        FoodProperties dumplings = FoodValues.DUMPLINGS;
                        return createFood(dumplings.nutrition() / 4, dumplings.saturation() / 4, dumplings);
                    }),
                    makeEntryForFood(ModItems.STUFFED_POTATO, item -> {
                        FoodProperties stuffedPotato = FoodValues.STUFFED_POTATO;
                        return createFood(stuffedPotato.nutrition() / 3, stuffedPotato.saturation() / 3, stuffedPotato);
                    })
            );
        }

        public static List<DietEntry> seaDiet(){
            return List.of(
                    makeEntryForFood(ModItems.CABBAGE_ROLLS, item -> {
                        FoodProperties cabbageRoll = FoodValues.CABBAGE_ROLLS;
                        return createFood(cabbageRoll.nutrition() / 2, cabbageRoll.saturation() / 2, cabbageRoll);
                    }),
                    makeEntryForFood(ModItems.SALMON_ROLL, item -> {
                        FoodProperties salmonRoll = FoodValues.SALMON_ROLL;
                        return createFood(salmonRoll.nutrition() / 3 * 2, salmonRoll.saturation() / 3 * 2, salmonRoll);
                    }),
                    makeEntryForFood(ModItems.COD_ROLL, item -> {
                        FoodProperties codRoll = FoodValues.COD_ROLL;
                        return createFood(codRoll.nutrition() / 3 * 2, codRoll.saturation() / 3 * 2, codRoll);
                    }),
                    makeEntryForFood(ModItems.FISH_STEW, item -> {
                        FoodProperties fishStew = FoodValues.FISH_STEW;
                        return createFood(fishStew.nutrition() / 3, fishStew.saturation() / 3, fishStew);
                    }),
                    makeEntryForFood(ModItems.BAKED_COD_STEW, item -> {
                        FoodProperties bakedCodStew = FoodValues.BAKED_COD_STEW;
                        return createFood(bakedCodStew.nutrition() / 4, bakedCodStew.saturation() / 4, bakedCodStew);
                    }),
                    makeEntryForFood(ModItems.PASTA_WITH_MEATBALLS, item -> {
                        FoodProperties pastaMeatballs = FoodValues.PASTA_WITH_MEATBALLS;
                        return createFood(pastaMeatballs.nutrition() / 2, pastaMeatballs.saturation() / 2, pastaMeatballs);
                    }),
                    makeEntryForFood(ModItems.PASTA_WITH_MUTTON_CHOP, item -> {
                        FoodProperties pastaMutton = FoodValues.PASTA_WITH_MUTTON_CHOP;
                        return createFood(pastaMutton.nutrition() / 2, pastaMutton.saturation() / 2, pastaMutton);
                    }),
                    makeEntryForFood(ModItems.STUFFED_POTATO, item -> {
                        FoodProperties stuffedPotato = FoodValues.STUFFED_POTATO;
                        return createFood(stuffedPotato.nutrition() / 3, stuffedPotato.saturation() / 3, stuffedPotato);
                    })
            );
        }

        public static List<DietEntry> tundraDiet(){
            return List.of(
                    makeEntryForFood(ModItems.BARBECUE_STICK, item ->
                            createFood(
                                    FoodValues.BARBECUE_STICK.nutrition() / 3,
                                    FoodValues.BARBECUE_STICK.saturation() / 3)),
                    makeEntryForFood(ModItems.BEEF_STEW, item -> {
                        FoodProperties beefStew = FoodValues.BEEF_STEW;
                        return createFood(beefStew.nutrition() / 3, beefStew.saturation() / 3, beefStew);
                    }),
                    makeEntryForFood(ModItems.CHICKEN_SOUP, item -> {
                        FoodProperties chickenSoup = FoodValues.CHICKEN_SOUP;
                        return createFood(chickenSoup.nutrition() / 4, chickenSoup.saturation() / 4, chickenSoup);
                    }),
                    makeEntryForFood(ModItems.PUMPKIN_SOUP, item -> {
                        FoodProperties pumpkinSoup = FoodValues.PUMPKIN_SOUP;
                        return createFood(pumpkinSoup.nutrition() / 4, pumpkinSoup.saturation() / 4, pumpkinSoup);
                    }),
                    makeEntryForFood(ModItems.HONEY_GLAZED_HAM, item -> {
                        FoodProperties honeyHam = FoodValues.HONEY_GLAZED_HAM;
                        return createFood(honeyHam.nutrition() / 8, honeyHam.saturation() / 8, honeyHam);
                    }),
                    makeEntryForFood(ModItems.NOODLE_SOUP, item -> {
                        FoodProperties noodleSoup = FoodValues.NOODLE_SOUP;
                        return createFood(noodleSoup.nutrition() / 2, noodleSoup.saturation() / 2, noodleSoup);
                    }),
                    makeEntryForFood(ModItems.CABBAGE_ROLLS, item -> {
                        FoodProperties cabbageRoll = FoodValues.CABBAGE_ROLLS;
                        return createFood(cabbageRoll.nutrition() / 2, cabbageRoll.saturation() / 2, cabbageRoll);
                    }),
                    makeEntryForFood(ModItems.SALMON_ROLL, item -> {
                        FoodProperties salmonRoll = FoodValues.SALMON_ROLL;
                        return createFood(salmonRoll.nutrition() / 3 * 2, salmonRoll.saturation() / 3 * 2, salmonRoll);
                    }),
                    makeEntryForFood(ModItems.COD_ROLL, item -> {
                        FoodProperties codRoll = FoodValues.COD_ROLL;
                        return createFood(codRoll.nutrition() / 3 * 2, codRoll.saturation() / 3 * 2, codRoll);
                    }),
                    makeEntryForFood(ModItems.FISH_STEW, item -> {
                        FoodProperties fishStew = FoodValues.FISH_STEW;
                        return createFood(fishStew.nutrition() / 3, fishStew.saturation() / 3, fishStew);
                    }),
                    makeEntryForFood(ModItems.BAKED_COD_STEW, item -> {
                        FoodProperties bakedCodStew = FoodValues.BAKED_COD_STEW;
                        return createFood(bakedCodStew.nutrition() / 4, bakedCodStew.saturation() / 4, bakedCodStew);
                    }),
                    makeEntryForFood(ModItems.CHICKEN_SANDWICH, item -> {
                        FoodProperties sandwich = FoodValues.CHICKEN_SANDWICH;
                        return createFood(sandwich.nutrition() / 4, sandwich.saturation() / 4, sandwich);
                    }),
                    makeEntryForFood(ModItems.HAMBURGER, item -> {
                        FoodProperties hamburger = FoodValues.HAMBURGER;
                        return createFood(hamburger.nutrition() / 6, hamburger.saturation() / 6, hamburger);
                    }),
                    makeEntryForFood(ModItems.BACON_SANDWICH, item -> {
                        FoodProperties baconSandwich = FoodValues.BACON_SANDWICH;
                        return createFood(baconSandwich.nutrition() / 4, baconSandwich.saturation() / 4, baconSandwich);
                    }),
                    makeEntryForFood(ModItems.MUTTON_WRAP, item -> {
                        FoodProperties muttonWrap = FoodValues.MUTTON_WRAP;
                        return createFood(muttonWrap.nutrition() / 4, muttonWrap.saturation() / 4, muttonWrap);
                    }),
                    makeEntryForFood(ModItems.DUMPLINGS, item -> {
                        FoodProperties dumplings = FoodValues.DUMPLINGS;
                        return createFood(dumplings.nutrition() / 4, dumplings.saturation() / 4, dumplings);
                    }),
                    makeEntryForFood(ModItems.STUFFED_POTATO, item -> {
                        FoodProperties stuffedPotato = FoodValues.STUFFED_POTATO;
                        return createFood(stuffedPotato.nutrition() / 3 * 2, stuffedPotato.saturation() / 3 * 2, stuffedPotato);
                    }),
                    makeEntryForFood(ModItems.BACON_AND_EGGS, item -> {
                        FoodProperties baconEggs = FoodValues.BACON_AND_EGGS;
                        return createFood(baconEggs.nutrition() / 2, baconEggs.saturation() / 2, baconEggs);
                    }),
                    makeEntryForFood(ModItems.PASTA_WITH_MEATBALLS, item -> {
                        FoodProperties pastaMeatballs = FoodValues.PASTA_WITH_MEATBALLS;
                        return createFood(pastaMeatballs.nutrition(), pastaMeatballs.saturation(), pastaMeatballs);
                    }),
                    makeEntryForFood(ModItems.PASTA_WITH_MUTTON_CHOP, item -> {
                        FoodProperties pastaMutton = FoodValues.PASTA_WITH_MUTTON_CHOP;
                        return createFood(pastaMutton.nutrition(), pastaMutton.saturation(), pastaMutton);
                    }),
                    makeEntryForFood(ModItems.SQUID_INK_PASTA, item -> {
                        FoodProperties pastaSquid = FoodValues.SQUID_INK_PASTA;
                        return createFood(pastaSquid.nutrition() / 2, pastaSquid.saturation() / 2, pastaSquid);
                    }),
                    makeEntryForFood(ModItems.GRILLED_SALMON, item -> {
                        FoodProperties grilledSalmon = FoodValues.GRILLED_SALMON;
                        return createFood(grilledSalmon.nutrition() / 4, grilledSalmon.saturation() / 4, grilledSalmon);
                    }),
                    makeEntryForFood(ModItems.ROAST_CHICKEN, item -> {
                        FoodProperties roastChicken = FoodValues.ROAST_CHICKEN;
                        return createFood(roastChicken.nutrition() / 8, roastChicken.saturation() / 8, roastChicken);
                    }),
                    makeEntryForFood(ModItems.SHEPHERDS_PIE, item -> {
                        FoodProperties pie = FoodValues.SHEPHERDS_PIE;
                        return createFood(pie.nutrition() / 5 * 3, pie.saturation() / 5 * 3, pie);
                    })
            );
        }

        public static List<DietEntry> aetherDiet(){
            return List.of(
                    makeEntryForFood(ModItems.SWEET_BERRY_COOKIE, item ->
                            createFood(FoodValues.COOKIES)),
                    makeEntryForFood(ModItems.HONEY_COOKIE, item ->
                            createFood(FoodValues.COOKIES)),
                    makeEntryForFood(ModItems.GLOW_BERRY_CUSTARD, item -> {
                        FoodProperties glowBerry = FoodValues.GLOW_BERRY_CUSTARD;
                        return createFood(glowBerry.nutrition() / 4 * 3, glowBerry.saturation() / 4 * 3, glowBerry);
                    }),
                    makeEntryForFood(ModItems.BEEF_STEW, item -> {
                        FoodProperties beefStew = FoodValues.BEEF_STEW;
                        return createFood(beefStew.nutrition() / 3, beefStew.saturation() / 3, beefStew);
                    }),
                    makeEntryForFood(ModItems.CHICKEN_SOUP, item -> {
                        FoodProperties chickenSoup = FoodValues.CHICKEN_SOUP;
                        return createFood(chickenSoup.nutrition() / 4, chickenSoup.saturation() / 4, chickenSoup);
                    }),
                    makeEntryForFood(ModItems.PUMPKIN_SOUP, item -> {
                        FoodProperties pumpkinSoup = FoodValues.PUMPKIN_SOUP;
                        return createFood(pumpkinSoup.nutrition() / 2, pumpkinSoup.saturation() / 2, pumpkinSoup);
                    }),
                    makeEntryForFood(ModItems.HONEY_GLAZED_HAM, item -> {
                        FoodProperties honeyHam = FoodValues.HONEY_GLAZED_HAM;
                        return createFood(honeyHam.nutrition() / 8, honeyHam.saturation() / 8, honeyHam);
                    }),
                    makeEntryForFood(ModItems.CABBAGE_ROLLS, item -> {
                        FoodProperties cabbageRoll = FoodValues.CABBAGE_ROLLS;
                        return createFood(cabbageRoll.nutrition() / 2, cabbageRoll.saturation() / 2, cabbageRoll);
                    }),
                    makeEntryForFood(ModItems.NOODLE_SOUP, item -> {
                        FoodProperties noodleSoup = FoodValues.NOODLE_SOUP;
                        return createFood(noodleSoup.nutrition() / 2, noodleSoup.saturation() / 2, noodleSoup);
                    }),
                    makeEntryForFood(ModItems.SALMON_ROLL, item -> {
                        FoodProperties salmonRoll = FoodValues.SALMON_ROLL;
                        return createFood(salmonRoll.nutrition() / 3 * 2, salmonRoll.saturation() / 3 * 2, salmonRoll);
                    }),
                    makeEntryForFood(ModItems.COD_ROLL, item -> {
                        FoodProperties codRoll = FoodValues.COD_ROLL;
                        return createFood(codRoll.nutrition() / 3 * 2, codRoll.saturation() / 3 * 2, codRoll);
                    }),
                    makeEntryForFood(ModItems.FISH_STEW, item -> {
                        FoodProperties fishStew = FoodValues.FISH_STEW;
                        return createFood(fishStew.nutrition() / 3, fishStew.saturation() / 3, fishStew);
                    }),
                    makeEntryForFood(ModItems.BAKED_COD_STEW, item -> {
                        FoodProperties bakedCodStew = FoodValues.BAKED_COD_STEW;
                        return createFood(bakedCodStew.nutrition() / 2, bakedCodStew.saturation() / 2, bakedCodStew);
                    }),
                    makeEntryForFood(ModItems.DUMPLINGS, item -> {
                        FoodProperties dumplings = FoodValues.DUMPLINGS;
                        return createFood(dumplings.nutrition() / 4, dumplings.saturation() / 4, dumplings);
                    }),
                    makeEntryForFood(ModItems.STUFFED_POTATO, item -> {
                        FoodProperties stuffedPotato = FoodValues.STUFFED_POTATO;
                        return createFood(stuffedPotato.nutrition() / 3, stuffedPotato.saturation() / 3, stuffedPotato);
                    }),
                    makeEntryForFood(ModItems.PASTA_WITH_MEATBALLS, item -> {
                        FoodProperties pastaMeatballs = FoodValues.PASTA_WITH_MEATBALLS;
                        return createFood(pastaMeatballs.nutrition() / 3 * 2, pastaMeatballs.saturation() / 3 * 2, pastaMeatballs);
                    }),
                    makeEntryForFood(ModItems.PASTA_WITH_MUTTON_CHOP, item -> {
                        FoodProperties pastaMutton = FoodValues.PASTA_WITH_MUTTON_CHOP;
                        return createFood(pastaMutton.nutrition() / 3 * 2, pastaMutton.saturation() / 3 * 2, pastaMutton);
                    }),
                    makeEntryForFood(ModItems.BARBECUE_STICK, item ->
                            createFood(
                                    FoodValues.BARBECUE_STICK.nutrition() / 3,
                                    FoodValues.BARBECUE_STICK.saturation() / 3)),
                    makeEntryForFood(ModItems.SQUID_INK_PASTA, item -> {
                        FoodProperties pastaSquid = FoodValues.SQUID_INK_PASTA;
                        return createFood(pastaSquid.nutrition() / 4, pastaSquid.saturation() / 4, pastaSquid);
                    }),
                    makeEntryForFood(ModItems.SHEPHERDS_PIE, item -> {
                        FoodProperties pie = FoodValues.SHEPHERDS_PIE;
                        return createFood(pie.nutrition() / 5, pie.saturation() / 5, pie);
                    })
            );
        }

        @Override
        public @NotNull String getName() {
            return "Dragon's Delight Diet Entries";
        }
    }
}
