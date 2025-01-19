package net.michael.openplease;


import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Mod(OpenPlease.MOD_ID)
public class OpenPlease {
    public static final String MOD_ID = "openplease";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static KeyMapping toggleOpen;
    public static KeyMapping toggleSound;
    public static KeyMapping getState;

    private boolean previousKeyStateOpen = false; // Track key state for toggling
    private boolean previousKeyStateSound = false; // Track key state for toggling
    private boolean previousKeyGetSate = false; // Track key state for getting state
    public static boolean doorToggle = true; // Toggle for auto-open feature
    public static boolean soundToggle = true; // Toggle for sound feature
    public static final float DOOR_DISTANCE = 2f; // Distance for door interaction

    public OpenPlease() {
        IEventBus modEventBus = ModLoadingContext.get().getActiveContainer().getEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Common setup logic (currently empty)
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Add items or blocks to creative mode tabs (currently empty)
    }

    // @SubscribeEvent
    public void registerKeyMappings(RegisterKeyMappingsEvent event) {
        toggleOpen = new KeyMapping(
                "key.openplease.toggleopen",
                GLFW.GLFW_KEY_R,
                "key.categories.openplease"
        );
        toggleSound = new KeyMapping(
                "key.openplease.togglesound",
                GLFW.GLFW_KEY_Y,
                "key.categories.openplease"
        );
        getState = new KeyMapping(
                "key.openplease.getstate",
                GLFW.GLFW_KEY_U,
                "key.categories.openplease"
        );

        event.register(toggleOpen);
        event.register(toggleSound);
        event.register(getState);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("OpenPlease: Server is starting...");
    }

    @SubscribeEvent
    public void onServerTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide) {
            boolean currentKeyStateOpen = toggleOpen.isDown();
            boolean currentKeyStateSound = toggleSound.isDown();
            boolean currentKeyGetState = getState.isDown();
            if (currentKeyStateOpen && !previousKeyStateOpen) {
                doorToggle = !doorToggle;

                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.literal(doorToggle ? "Auto-Open Enabled!" : "Auto-Open Disabled!")
                                .withStyle(doorToggle ? ChatFormatting.GREEN : ChatFormatting.RED),
                        true
                );
            }
            if (currentKeyStateSound && !previousKeyStateSound) {
                soundToggle = !soundToggle;

                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.literal(soundToggle ? "Sound Enabled!" : "Sound Disabled!")
                                .withStyle(soundToggle ? ChatFormatting.GREEN : ChatFormatting.RED),
                        true
                );
            }
            if (currentKeyGetState && !previousKeyGetSate) {
                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.literal("Auto-Open: ").withStyle(ChatFormatting.WHITE)
                                .append(Component.literal(doorToggle ? "Enabled" : "Disabled")
                                        .withStyle(doorToggle ? ChatFormatting.GREEN : ChatFormatting.RED))
                                .append(Component.literal(", Sound: ").withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(soundToggle ? "Enabled" : "Disabled")
                                        .withStyle(soundToggle ? ChatFormatting.GREEN : ChatFormatting.RED)),
                        true
                );
            }

            previousKeyStateOpen = currentKeyStateOpen;
            previousKeyStateSound = currentKeyStateSound;
            previousKeyGetSate = currentKeyGetState;
        }

        if (event.getLevel() instanceof ServerLevel world && doorToggle) {
            for (Player player : world.players()) {
                BlockPos playerPos = player.blockPosition();

                for (int x = -4; x <= 4; x++) {
                    for (int y = -4; y <= 4; y++) {
                        for (int z = -4; z <= 4; z++) {
                            BlockPos pos = playerPos.offset(x, y, z);
                            Block block = world.getBlockState(pos).getBlock();

                            if (block instanceof DoorBlock && block != Blocks.IRON_DOOR) {
                                boolean oldStateDoor = world.getBlockState(pos).getValue(DoorBlock.OPEN);
                                handleDoor(world, pos, playerPos);
                                if (oldStateDoor && !world.getBlockState(pos).getValue(DoorBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                                if (!oldStateDoor && world.getBlockState(pos).getValue(DoorBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                            }
                            if (block instanceof TrapDoorBlock && block != Blocks.IRON_TRAPDOOR) {
                                boolean oldStateTrapDoor = world.getBlockState(pos).getValue(TrapDoorBlock.OPEN);
                                handleTrapdoor(world, pos, playerPos);
                                if (oldStateTrapDoor && !world.getBlockState(pos).getValue(TrapDoorBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                                if (!oldStateTrapDoor && world.getBlockState(pos).getValue(TrapDoorBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                            }
                            if (block instanceof FenceGateBlock) {
                                boolean oldStateFenceGate = world.getBlockState(pos).getValue(FenceGateBlock.OPEN);
                                handleFenceGate(world, pos, playerPos);
                                if (oldStateFenceGate && !world.getBlockState(pos).getValue(FenceGateBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                                if (!oldStateFenceGate && world.getBlockState(pos).getValue(FenceGateBlock.OPEN) && soundToggle) {
                                    world.playSound(null, pos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleDoor(ServerLevel world, BlockPos doorPos, BlockPos playerPos) {
        double distance = playerPos.distSqr(doorPos);
        boolean isOpen = world.getBlockState(doorPos).getValue(DoorBlock.OPEN);
        boolean shouldBeOpen = distance <= DOOR_DISTANCE + 2 * DOOR_DISTANCE + 2;

        if (isOpen != shouldBeOpen) {
            world.setBlock(doorPos, world.getBlockState(doorPos).setValue(DoorBlock.OPEN, shouldBeOpen), 3);
        }
    }

    private void handleTrapdoor(ServerLevel world, BlockPos trapdoorPos, BlockPos playerPos) {
        double distance = playerPos.distSqr(trapdoorPos);
        boolean isOpen = world.getBlockState(trapdoorPos).getValue(TrapDoorBlock.OPEN);
        boolean shouldBeOpen = distance <= DOOR_DISTANCE * DOOR_DISTANCE;

        if (isOpen != shouldBeOpen) {
            world.setBlock(trapdoorPos, world.getBlockState(trapdoorPos).setValue(TrapDoorBlock.OPEN, shouldBeOpen), 3);
        }
    }

    private void handleFenceGate(ServerLevel world, BlockPos fenceGatePos, BlockPos playerPos) {
        double distance = playerPos.distSqr(fenceGatePos);
        boolean isOpen = world.getBlockState(fenceGatePos).getValue(FenceGateBlock.OPEN);
        boolean shouldBeOpen = distance <= DOOR_DISTANCE * DOOR_DISTANCE;

        if (isOpen != shouldBeOpen) {
            world.setBlock(fenceGatePos, world.getBlockState(fenceGatePos).setValue(FenceGateBlock.OPEN, shouldBeOpen), 3);
        }
    }

}
