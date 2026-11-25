/**
 * Mixins. Register each class in {@code mountarmortweaks.mixins.json}: under {@code "mixins"} if it
 * targets code that exists on both sides, under {@code "client"} if it targets client-only code.
 *
 * <p>Conventions:
 * <ul>
 *   <li>Prefix every added member with {@code mountarmortweaks$} so it cannot clash with other mods.</li>
 *   <li>Prefer {@code @Inject} and MixinExtras ({@code @WrapOperation}, {@code @ModifyExpressionValue})
 *       over {@code @Redirect} and {@code @Overwrite}, which fail or silently lose when another mod
 *       (Lithium, Carpet, ...) touches the same call.</li>
 *   <li>State in each mixin's Javadoc what it changes and any known interaction with other mods.</li>
 * </ul>
 */
package com.ceruleax.mountarmortweaks.mixin;
