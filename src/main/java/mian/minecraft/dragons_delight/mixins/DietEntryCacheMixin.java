package mian.minecraft.dragons_delight.mixins;

import by.dragonsurvivalteam.dragonsurvival.registry.data_maps.DietEntryCache;
import by.dragonsurvivalteam.dragonsurvival.registry.dragon.DragonSpecies;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import mian.minecraft.dragons_delight.DragonsDelight;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(DietEntryCache.class)
public class DietEntryCacheMixin {

    @Shadow @Final private static Map<ResourceKey<DragonSpecies>, Map<Item, FoodProperties>> CACHE;

    // when checking for an item, we just see if it can be part of the diet based on our methods
    @Inject(method = "getDiet", at = @At("HEAD"))
    private static void getDiet(Holder<DragonSpecies> species, Item item, CallbackInfoReturnable<FoodProperties> cir){
        Map<Item, FoodProperties> map = CACHE.get(species.getKey());
        if(map.get(item) == null && item.components().has(DataComponents.FOOD)){
            DragonsDelight.makeFoodEntryBasedOnDiet(
                    ServerLifecycleHooks.getCurrentServer(),
                    map.keySet().stream().toList(),
                    item).ifPresent(entry -> {
                        DragonsDelight.LOGGER.debug(item + " is added to " + species.getKey().location().getPath());
                        map.put(item, entry.properties().get());
                    });
        }
    }

}
