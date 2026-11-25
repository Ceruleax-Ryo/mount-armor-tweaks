package com.ceruleax.mountarmortweaks.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets body armor wear down on mounts that do not do it themselves.
 *
 * <p>{@code LivingEntity.hurtArmor} is empty. {@code Horse} and {@code Wolf} override it with
 * {@code doHurtEquipment(source, damage, BODY)}; zombie horses, nautili and zombie nautili do not.
 * This injects the same call into the empty base method for {@link AbstractHorse} and
 * {@link AbstractNautilus}. {@link Horse} is skipped so that a future {@code super.hurtArmor} call
 * cannot double the damage.
 *
 * <p>Nothing is damaged unless the {@code durability} option has given the armor {@code max_damage}
 * and {@code damage_on_hurt}: {@code doHurtEquipment} checks both. Llama carpets have neither.
 *
 * <p>Injecting at HEAD (rather than adding overrides to the subclasses) keeps other mods' injects into
 * this method working. Not yet tested together with Lithium or Carpet.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Shadow
    protected abstract void doHurtEquipment(DamageSource damageSource, float damage, EquipmentSlot... slots);

    @Inject(method = "hurtArmor", at = @At("HEAD"))
    private void mountarmortweaks$hurtMountArmor(DamageSource damageSource, float damage, CallbackInfo ci) {
        Object self = this;
        if ((self instanceof AbstractHorse && !(self instanceof Horse)) || self instanceof AbstractNautilus) {
            this.doHurtEquipment(damageSource, damage, EquipmentSlot.BODY);
        }
    }
}
