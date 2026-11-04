package dev.boomys.liminaldoors.entity;

import java.util.List;

import dev.boomys.liminaldoors.LiminalManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A tall, silent figure that only exists inside the liminal rooms.
 * It waits in the dark, walks towards you when it notices you, and occasionally
 * blinks to a spot just behind you while you are not looking at it.
 */
public class StalkerEntity extends HostileEntity {
	private static final double NOTICE_RANGE = 42.0;
	private static final double BLINK_MIN = 10.0;
	private static final double BLINK_MAX = 34.0;

	public StalkerEntity(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
		this.experiencePoints = 8;
	}

	public static DefaultAttributeContainer.Builder createStalkerAttributes() {
		return HostileEntity.createHostileAttributes()
				.add(EntityAttributes.MAX_HEALTH, 60.0)
				.add(EntityAttributes.MOVEMENT_SPEED, 0.27)
				.add(EntityAttributes.ATTACK_DAMAGE, 6.0)
				.add(EntityAttributes.FOLLOW_RANGE, 96.0)
				.add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.9);
	}

	@Override
	protected void initGoals() {
		this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));
		this.goalSelector.add(3, new WanderAroundFarGoal(this, 1.0));
		this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 48.0F));
		this.goalSelector.add(5, new LookAroundGoal(this));
		this.targetSelector.add(1, new RevengeGoal(this));
		this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
	}

	private boolean isValidPrey(LivingEntity entity) {
		return entity instanceof PlayerEntity player
				&& !player.isCreative()
				&& !player.isSpectator()
				&& LiminalManager.isInLiminal(player);
	}

	@Override
	public void tick() {
		super.tick();
		World world = this.getWorld();
		if (world.isClient()) {
			return;
		}

		if (this.age % 20 == 0) {
			this.searchForPrey((ServerWorld) world);
		}

		if (this.getTarget() instanceof ServerPlayerEntity player) {
			double distanceSq = this.squaredDistanceTo(player);

			if (distanceSq < 64.0 && this.age % 45 == 0) {
				world.playSound(null, player.getX(), player.getY(), player.getZ(),
						SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.4F, 1.0F);
			}

			if (distanceSq > BLINK_MIN * BLINK_MIN && distanceSq < BLINK_MAX * BLINK_MAX
					&& this.random.nextInt(220) == 0 && !this.canSee(player)) {
				this.blinkBehind(player);
			}

			if (distanceSq < 25.0 && this.age % 60 == 0) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 120, 0, false, false));
			}
		}
	}

	private void searchForPrey(ServerWorld world) {
		if (this.getTarget() != null && this.getTarget().isAlive()) {
			return;
		}

		List<ServerPlayerEntity> players = world.getPlayers(player -> player.isAlive() && this.isValidPrey(player));
		ServerPlayerEntity closest = null;
		double best = NOTICE_RANGE * NOTICE_RANGE;

		for (ServerPlayerEntity player : players) {
			double distanceSq = this.squaredDistanceTo(player);
			if (distanceSq < best) {
				best = distanceSq;
				closest = player;
			}
		}

		if (closest != null && this.random.nextInt(3) == 0) {
			this.setTarget(closest);
			world.playSound(null, this.getX(), this.getY(), this.getZ(),
					SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.HOSTILE, 1.6F, 0.55F);
		}
	}

	private void blinkBehind(PlayerEntity player) {
		Vec3d look = player.getRotationVec(1.0F);
		double x = player.getX() - look.x * 5.0;
		double z = player.getZ() - look.z * 5.0;

		this.refreshPositionAndAngles(x, player.getY(), z, this.getYaw(), this.getPitch());
		this.getWorld().playSound(null, x, player.getY(), z,
				SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.2F, 0.5F);
	}

	@Override
	public boolean damage(ServerWorld world, net.minecraft.entity.damage.DamageSource source, float amount) {
		boolean hurt = super.damage(world, source, amount);
		if (hurt && this.getTarget() == null && source.getAttacker() instanceof PlayerEntity player) {
			this.setTarget(player);
		}
		return hurt;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	@Override
	public boolean isPersistent() {
		return true;
	}
}
