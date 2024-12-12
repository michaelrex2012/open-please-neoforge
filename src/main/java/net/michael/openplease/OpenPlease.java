package net.michael.openplease;


import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLServiceProvider;
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
    public static KeyMapping toggle;

    private boolean previousKeyState = false; // Track key state for toggling
    public static boolean doorToggle = true; // Toggle for auto-open feature
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
        toggle = new KeyMapping(
                "key.openplease.toggle",
                GLFW.GLFW_KEY_R,
                "key.categories.openplease"
        );

        event.register(toggle);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("OpenPlease: Server is starting...");
    }

    @SubscribeEvent
    public void onServerTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide) {
            boolean currentKeyState = toggle.isDown();
            if (currentKeyState && !previousKeyState) {
                doorToggle = !doorToggle;

                Minecraft.getInstance().gui.setOverlayMessage(
                        Component.literal(doorToggle ? "Auto-Open Enabled!" : "Auto-Open Disabled!")
                                .withStyle(doorToggle ? ChatFormatting.GREEN : ChatFormatting.RED),
                        true
                );
            }
            previousKeyState = currentKeyState;
            return;
        }

        if (event.getLevel() instanceof ServerLevel world && doorToggle) {
            for (Player player : world.players()) {
                BlockPos playerPos = player.blockPosition();

                for (int x = -4; x <= 4; x++) {
                    for (int y = -4; y <= 4; y++) {
                        for (int z = -4; z <= 4; z++) {
                            BlockPos pos = playerPos.offset(x, y, z);
                            Block block = world.getBlockState(pos).getBlock();

                            if (block instanceof DoorBlock) {
                                handleDoor(world, pos, playerPos);
                            } else if (block instanceof TrapDoorBlock) {
                                handleTrapdoor(world, pos, playerPos);
                            } else if (block instanceof FenceGateBlock) {
                                handleFenceGate(world, pos, playerPos);
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
        boolean shouldBeOpen = distance <= DOOR_DISTANCE * DOOR_DISTANCE;

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
