/*
 * MIT License
 *
 * Copyright (c) 2024 GeckoLib
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.zigythebird.playeranim.molang;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranimcore.animation.AnimationController;
import com.zigythebird.playeranimcore.molang.MolangLoader;
import com.zigythebird.playeranimcore.molang.QueryBinding;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientMannequin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.Optional;

public final class MolangQueries {
    public static final String ACTOR_COUNT = "actor_count";
    public static final String BLOCKING = "blocking";
    public static final String BODY_X_ROTATION = "body_x_rotation";
    public static final String BODY_Y_ROTATION = "body_y_rotation";
    public static final String CARDINAL_FACING = "cardinal_facing";
    public static final String CARDINAL_FACING_2D = "cardinal_facing_2d";
    public static final String CARDINAL_PLAYER_FACING = "cardinal_player_facing";
    public static final String DAY = "day";
    public static final String DEATH_TICKS = "death_ticks";
    public static final String DISTANCE_FROM_CAMERA = "distance_from_camera";
    public static final String EQUIPMENT_COUNT = "equipment_count";
    public static final String FRAME_ALPHA = "frame_alpha";
    public static final String GET_ACTOR_INFO_ID = "get_actor_info_id";
    public static final String GROUND_SPEED = "ground_speed";
    public static final String HAS_CAPE = "has_cape";
    public static final String HAS_COLLISION = "has_collision";
    public static final String HAS_GRAVITY = "has_gravity";
    public static final String HAS_HEAD_GEAR = "has_head_gear";
    public static final String HAS_OWNER = "has_owner";
    public static final String HAS_PLAYER_RIDER = "has_player_rider";
    public static final String HAS_RIDER = "has_rider";
    public static final String HEAD_X_ROTATION = "head_x_rotation";
    public static final String HEAD_Y_ROTATION = "head_y_rotation";
    public static final String HEALTH = "health";
    public static final String HURT_TIME = "hurt_time";
    public static final String INVULNERABLE_TICKS = "invulnerable_ticks";
    public static final String IS_ALIVE = "is_alive";
    public static final String IS_ANGRY = "is_angry";
    public static final String IS_BABY = "is_baby";
    public static final String IS_BREATHING = "is_breathing";
    public static final String IS_FIRE_IMMUNE = "is_fire_immune";
    public static final String IS_FIRST_PERSON = "is_first_person";
    public static final String IS_IN_CONTACT_WITH_WATER = "is_in_contact_with_water";
    public static final String IS_IN_LAVA = "is_in_lava";
    public static final String IS_IN_WATER = "is_in_water";
    public static final String IS_IN_WATER_OR_RAIN = "is_in_water_or_rain";
    public static final String IS_INVISIBLE = "is_invisible";
    public static final String IS_LEASHED = "is_leashed";
    public static final String IS_MOVING = "is_moving";
    public static final String IS_ON_FIRE = "is_on_fire";
    public static final String IS_ON_GROUND = "is_on_ground";
    public static final String IS_RIDING = "is_riding";
    public static final String IS_SADDLED = "is_saddled";
    public static final String IS_SILENT = "is_silent";
    public static final String IS_SLEEPING = "is_sleeping";
    public static final String IS_SNEAKING = "is_sneaking";
    public static final String IS_SPRINTING = "is_sprinting";
    public static final String IS_SWIMMING = "is_swimming";
    public static final String IS_USING_ITEM = "is_using_item";
    public static final String IS_WALL_CLIMBING = "is_wall_climbing";
    public static final String LIFE_TIME = "life_time";
    public static final String LIMB_SWING = "limb_swing";
    public static final String LIMB_SWING_AMOUNT = "limb_swing_amount";
    public static final String MAIN_HAND_ITEM_MAX_DURATION = "main_hand_item_max_duration";
    public static final String MAIN_HAND_ITEM_USE_DURATION = "main_hand_item_use_duration";
    public static final String MAX_HEALTH = "max_health";
    public static final String MOON_BRIGHTNESS = "moon_brightness";
    public static final String MOON_PHASE = "moon_phase";
    public static final String MOVEMENT_DIRECTION = "movement_direction";
    public static final String PLAYER_LEVEL = "player_level";
    public static final String RIDER_BODY_X_ROTATION = "rider_body_x_rotation";
    public static final String RIDER_BODY_Y_ROTATION = "rider_body_y_rotation";
    public static final String RIDER_HEAD_X_ROTATION = "rider_head_x_rotation";
    public static final String RIDER_HEAD_Y_ROTATION = "rider_head_y_rotation";
    public static final String SCALE = "scale";
    public static final String SLEEP_ROTATION = "sleep_rotation";
    public static final String TIME_OF_DAY = "time_of_day";
    public static final String TIME_STAMP = "time_stamp";
    public static final String VERTICAL_SPEED = "vertical_speed";
    public static final String YAW_SPEED = "yaw_speed";

    public static void setDefaultQueryValues(QueryBinding<AnimationController> binding) {
        MolangLoader.setDoubleQuery(binding, ACTOR_COUNT, actor -> Minecraft.getInstance().levelRenderer.levelRenderState.entityRenderStates.size());
        MolangLoader.setDoubleQuery(binding, CARDINAL_PLAYER_FACING, actor -> ((PlayerAnimationController) actor).getAvatar().getDirection().ordinal());
        MolangLoader.setDoubleQuery(binding, DAY, actor -> ((PlayerAnimationController) actor).getAvatar().level().getGameTime() / 24000d);
        MolangLoader.setDoubleQuery(binding, FRAME_ALPHA, actor -> actor.getAnimationData().getPartialTick());
        MolangLoader.setBoolQuery(binding, HAS_CAPE, actor -> {
            Avatar avatar = ((PlayerAnimationController) actor).getAvatar();
            if (avatar instanceof AbstractClientPlayer player) {
                return player.getSkin().cape() != null;
            } else if (avatar instanceof ClientMannequin mannequin) {
                return mannequin.getSkin().cape() != null;
            } else {
                return false;
            }
        });
        MolangLoader.setBoolQuery(binding, IS_FIRST_PERSON, actor -> {
            Avatar avatar = ((PlayerAnimationController) actor).getAvatar();
            if (avatar instanceof AbstractClientPlayer player && player.isLocalPlayer()) {
                return Minecraft.getInstance().options.getCameraType() == CameraType.FIRST_PERSON;
            } else {
                return false;
            }
        });
        MolangLoader.setDoubleQuery(binding, LIFE_TIME, actor -> actor.isActive() ? actor.getAnimationTime() : 0);
        MolangLoader.setDoubleQuery(binding, MOON_BRIGHTNESS, actor -> {
            Avatar avatar = ((PlayerAnimationController) actor).getAvatar();
            return avatar.level().environmentAttributes().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, avatar.position());
        });
        MolangLoader.setDoubleQuery(binding, MOON_PHASE, actor -> {
            Avatar avatar = ((PlayerAnimationController) actor).getAvatar();
            return avatar.level().environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, avatar.position()).index();
        });
        MolangLoader.setDoubleQuery(binding, PLAYER_LEVEL, actor -> {
            Avatar avatar = ((PlayerAnimationController) actor).getAvatar();
            if (avatar instanceof AbstractClientPlayer player) {
                return player.experienceLevel;
            } else {
                return 0.0D;
            }
        });
        MolangLoader.setDoubleQuery(binding, TIME_OF_DAY, actor -> ((PlayerAnimationController) actor).getAvatar().level().getDefaultClockTime() / 24000d);
        MolangLoader.setDoubleQuery(binding, TIME_STAMP, actor -> ((PlayerAnimationController) actor).getAvatar().level().getGameTime());

        setDefaultEntityQueryValues(binding);
        setDefaultLivingEntityQueryValues(binding);
    }

    private static void setDefaultEntityQueryValues(QueryBinding<AnimationController> binding) {
        MolangLoader.setDoubleQuery(binding, BODY_X_ROTATION, actor -> ((PlayerAnimationController) actor).getAvatar().getViewXRot(actor.getAnimationData().getPartialTick()));
        MolangLoader.setDoubleQuery(binding, BODY_Y_ROTATION, actor -> ((PlayerAnimationController) actor).getAvatar() instanceof LivingEntity living ? Mth.lerp(actor.getAnimationData().getPartialTick(), living.yBodyRotO, living.yBodyRot) : ((PlayerAnimationController) actor).getAvatar().getViewYRot(actor.getAnimationData().getPartialTick()));
        MolangLoader.setDoubleQuery(binding, CARDINAL_FACING, actor -> ((PlayerAnimationController) actor).getAvatar().getDirection().get3DDataValue());
        MolangLoader.setDoubleQuery(binding, CARDINAL_FACING_2D, actor -> {
            int directionId = ((PlayerAnimationController) actor).getAvatar().getDirection().get3DDataValue();

            return directionId < 2 ? 6 : directionId;
        });
        MolangLoader.setDoubleQuery(binding, DISTANCE_FROM_CAMERA, actor -> Minecraft.getInstance().gameRenderer.getMainCamera().position().distanceTo(((PlayerAnimationController) actor).getAvatar().position()));
        MolangLoader.setDoubleQuery(binding, GET_ACTOR_INFO_ID, actor -> ((PlayerAnimationController) actor).getAvatar().getId());
        MolangLoader.setDoubleQuery(binding, EQUIPMENT_COUNT, actor -> ((PlayerAnimationController) actor).getAvatar() instanceof EquipmentUser armorable ? Arrays.stream(EquipmentSlot.values()).filter(EquipmentSlot::isArmor).filter(slot -> !armorable.getItemBySlot(slot).isEmpty()).count() : 0);
        MolangLoader.setBoolQuery(binding, HAS_COLLISION, actor -> !((PlayerAnimationController) actor).getAvatar().noPhysics);
        MolangLoader.setBoolQuery(binding, HAS_GRAVITY, actor -> !((PlayerAnimationController) actor).getAvatar().isNoGravity());
        MolangLoader.setBoolQuery(binding, HAS_OWNER, actor -> ((PlayerAnimationController) actor).getAvatar() instanceof OwnableEntity ownable && ownable.getOwnerReference() != null);
        MolangLoader.setBoolQuery(binding, HAS_PLAYER_RIDER, actor -> ((PlayerAnimationController) actor).getAvatar().hasPassenger(Player.class::isInstance));
        MolangLoader.setBoolQuery(binding, HAS_RIDER, actor -> ((PlayerAnimationController) actor).getAvatar().isVehicle());
        MolangLoader.setBoolQuery(binding, IS_ALIVE, actor -> ((PlayerAnimationController) actor).getAvatar().isAlive());
        MolangLoader.setBoolQuery(binding, IS_ANGRY, actor -> ((PlayerAnimationController) actor).getAvatar() instanceof NeutralMob neutralMob && neutralMob.isAngry());
        MolangLoader.setBoolQuery(binding, IS_BREATHING, actor -> ((PlayerAnimationController) actor).getAvatar().getAirSupply() >= ((PlayerAnimationController) actor).getAvatar().getMaxAirSupply());
        MolangLoader.setBoolQuery(binding, IS_FIRE_IMMUNE, actor -> ((PlayerAnimationController) actor).getAvatar().getType().fireImmune());
        MolangLoader.setBoolQuery(binding, IS_INVISIBLE, actor -> ((PlayerAnimationController) actor).getAvatar().isInvisible());
        MolangLoader.setBoolQuery(binding, IS_IN_CONTACT_WITH_WATER, actor -> ((PlayerAnimationController) actor).getAvatar().isInWaterOrRain());
        MolangLoader.setBoolQuery(binding, IS_IN_LAVA, actor -> ((PlayerAnimationController) actor).getAvatar().isInLava());
        MolangLoader.setBoolQuery(binding, IS_IN_WATER, actor -> ((PlayerAnimationController) actor).getAvatar().isInWater());
        MolangLoader.setBoolQuery(binding, IS_IN_WATER_OR_RAIN, actor -> ((PlayerAnimationController) actor).getAvatar().isInWaterOrRain());
        MolangLoader.setBoolQuery(binding, IS_LEASHED, actor -> ((PlayerAnimationController) actor).getAvatar() instanceof Leashable leashable && leashable.isLeashed());
        MolangLoader.setBoolQuery(binding, IS_MOVING, actor -> actor.getAnimationData().isMoving());
        MolangLoader.setBoolQuery(binding, IS_ON_FIRE, actor -> ((PlayerAnimationController) actor).getAvatar().isOnFire());
        MolangLoader.setBoolQuery(binding, IS_ON_GROUND, actor -> ((PlayerAnimationController) actor).getAvatar().onGround());
        MolangLoader.setBoolQuery(binding, IS_RIDING, actor -> ((PlayerAnimationController) actor).getAvatar().isPassenger());
        MolangLoader.setBoolQuery(binding, IS_SADDLED, actor -> ((PlayerAnimationController) actor).getAvatar() instanceof EquipmentUser saddleable && !saddleable.getItemBySlot(EquipmentSlot.SADDLE).isEmpty());
        MolangLoader.setBoolQuery(binding, IS_SILENT, actor -> ((PlayerAnimationController) actor).getAvatar().isSilent());
        MolangLoader.setBoolQuery(binding, IS_SNEAKING, actor -> ((PlayerAnimationController) actor).getAvatar().isCrouching());
        MolangLoader.setBoolQuery(binding, IS_SPRINTING, actor -> ((PlayerAnimationController) actor).getAvatar().isSprinting());
        MolangLoader.setBoolQuery(binding, IS_SWIMMING, actor -> ((PlayerAnimationController) actor).getAvatar().isSwimming());
        MolangLoader.setDoubleQuery(binding, MOVEMENT_DIRECTION, actor -> actor.getAnimationData().isMoving() ? Direction.getApproximateNearest(((PlayerAnimationController) actor).getAvatar().getDeltaMovement()).get3DDataValue() : 6);
        MolangLoader.setDoubleQuery(binding, RIDER_BODY_X_ROTATION, actor -> ((PlayerAnimationController) actor).getAvatar().isVehicle() ? ((PlayerAnimationController) actor).getAvatar().getFirstPassenger() instanceof LivingEntity ? 0 : ((PlayerAnimationController) actor).getAvatar().getFirstPassenger().getViewXRot(actor.getAnimationData().getPartialTick()) : 0);
        MolangLoader.setDoubleQuery(binding, RIDER_BODY_Y_ROTATION, actor -> ((PlayerAnimationController) actor).getAvatar().isVehicle() ? ((PlayerAnimationController) actor).getAvatar().getFirstPassenger() instanceof LivingEntity living ? Mth.lerp(actor.getAnimationData().getPartialTick(), living.yBodyRotO, living.yBodyRot) : ((PlayerAnimationController) actor).getAvatar().getFirstPassenger().getViewYRot(actor.getAnimationData().getPartialTick()) : 0);
        MolangLoader.setDoubleQuery(binding, RIDER_HEAD_X_ROTATION, actor -> ((PlayerAnimationController) actor).getAvatar().getFirstPassenger() instanceof LivingEntity living ? living.getViewXRot(actor.getAnimationData().getPartialTick()) : 0);
        MolangLoader.setDoubleQuery(binding, RIDER_HEAD_Y_ROTATION, actor -> ((PlayerAnimationController) actor).getAvatar().getFirstPassenger() instanceof LivingEntity living ? living.getViewYRot(actor.getAnimationData().getPartialTick()) : 0);
        MolangLoader.setDoubleQuery(binding, VERTICAL_SPEED, actor -> ((PlayerAnimationController) actor).getAvatar().getDeltaMovement().y);
        MolangLoader.setDoubleQuery(binding, YAW_SPEED, actor -> ((PlayerAnimationController) actor).getAvatar().getYRot() - ((PlayerAnimationController) actor).getAvatar().yRotO);
    }

    private static void setDefaultLivingEntityQueryValues(QueryBinding<AnimationController> binding) {
        MolangLoader.setBoolQuery(binding, BLOCKING, actor -> ((PlayerAnimationController) actor).getAvatar().isBlocking());
        MolangLoader.setDoubleQuery(binding, DEATH_TICKS, actor -> ((PlayerAnimationController) actor).getAvatar().deathTime == 0 ? 0 : ((PlayerAnimationController) actor).getAvatar().deathTime + actor.getAnimationData().getPartialTick());
        MolangLoader.setDoubleQuery(binding, GROUND_SPEED, actor -> ((PlayerAnimationController) actor).getAvatar().getDeltaMovement().horizontalDistance());
        MolangLoader.setBoolQuery(binding, HAS_HEAD_GEAR, actor -> !((PlayerAnimationController) actor).getAvatar().getItemBySlot(EquipmentSlot.HEAD).isEmpty());
        MolangLoader.setDoubleQuery(binding, HEAD_X_ROTATION, actor -> ((PlayerAnimationController) actor).getAvatar().getViewXRot(actor.getAnimationData().getPartialTick()));
        MolangLoader.setDoubleQuery(binding, HEAD_Y_ROTATION, actor -> ((PlayerAnimationController) actor).getAvatar().getViewYRot(actor.getAnimationData().getPartialTick()));
        MolangLoader.setDoubleQuery(binding, HEALTH, actor -> ((PlayerAnimationController) actor).getAvatar().getHealth());
        MolangLoader.setDoubleQuery(binding, HURT_TIME, actor -> ((PlayerAnimationController) actor).getAvatar().hurtTime == 0 ? 0 : ((PlayerAnimationController) actor).getAvatar().hurtTime - actor.getAnimationData().getPartialTick());
        MolangLoader.setDoubleQuery(binding, INVULNERABLE_TICKS, actor -> ((PlayerAnimationController) actor).getAvatar().invulnerableTime == 0 ? 0 : ((PlayerAnimationController) actor).getAvatar().invulnerableTime - actor.getAnimationData().getPartialTick());
        MolangLoader.setBoolQuery(binding, IS_BABY, actor -> ((PlayerAnimationController) actor).getAvatar().isBaby());
        MolangLoader.setBoolQuery(binding, IS_SLEEPING, actor -> ((PlayerAnimationController) actor).getAvatar().isSleeping());
        MolangLoader.setBoolQuery(binding, IS_USING_ITEM, actor -> ((PlayerAnimationController) actor).getAvatar().isUsingItem());
        MolangLoader.setBoolQuery(binding, IS_WALL_CLIMBING, actor -> ((PlayerAnimationController) actor).getAvatar().onClimbable());
        MolangLoader.setDoubleQuery(binding, LIMB_SWING, actor -> ((PlayerAnimationController) actor).getAvatar().walkAnimation.position());
        MolangLoader.setDoubleQuery(binding, LIMB_SWING_AMOUNT, actor -> ((PlayerAnimationController) actor).getAvatar().walkAnimation.speed(actor.getAnimationData().getPartialTick()));
        MolangLoader.setDoubleQuery(binding, MAIN_HAND_ITEM_MAX_DURATION, actor -> ((PlayerAnimationController) actor).getAvatar().getMainHandItem().getUseDuration(((PlayerAnimationController) actor).getAvatar()));
        MolangLoader.setDoubleQuery(binding, MAIN_HAND_ITEM_USE_DURATION, actor -> ((PlayerAnimationController) actor).getAvatar().getUsedItemHand() == InteractionHand.MAIN_HAND ? ((PlayerAnimationController) actor).getAvatar().getTicksUsingItem() / 20d + actor.getAnimationData().getPartialTick() : 0);
        MolangLoader.setDoubleQuery(binding, MAX_HEALTH, actor -> ((PlayerAnimationController) actor).getAvatar().getMaxHealth());
        MolangLoader.setDoubleQuery(binding, SCALE, actor -> ((PlayerAnimationController) actor).getAvatar().getScale());
        MolangLoader.setDoubleQuery(binding, SLEEP_ROTATION, actor -> Optional.ofNullable(((PlayerAnimationController) actor).getAvatar().getBedOrientation()).map(Direction::toYRot).orElse(0f));
    }
}
