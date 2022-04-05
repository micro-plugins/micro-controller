package com.sxtanna.mc.micro;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sxtanna.mc.micro.cmds.MicroControllerCommand;
import com.sxtanna.mc.micro.data.ControllerFlag;

import co.aikar.commands.PaperCommandManager;
import com.google.common.reflect.TypeToken;
import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.logging.Level;

public final class MicroControllerPlugin extends JavaPlugin implements Listener {

    @SuppressWarnings("UnstableApiUsage")
    private static final TypeToken<Map<String, Long>> TOKEN = new TypeToken<>() {

    };


    @Nullable
    private       PaperCommandManager    commands;
    @NotNull
    private final Object2LongMap<String> controlled = new Object2LongOpenHashMap<>();


    @Override
    public void onLoad() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("could not create necessary data directory, might cause issues");
        }
    }

    @Override
    public void onEnable() {
        loadControlled();

        getServer().getPluginManager().registerEvents(this, this);

        final var commands = this.commands = new PaperCommandManager(this);
        commands.enableUnstableAPI("help");
        commands.enableUnstableAPI("brigadier");
        commands.usePerIssuerLocale(true, true);

        commands.registerCommand(new MicroControllerCommand(this));
    }

    @Override
    public void onDisable() {
        saveControlled();

        HandlerList.unregisterAll(((Plugin) this));

        if (this.commands != null) {
            this.commands.unregisterCommands();
            this.commands = null;
        }

        this.controlled.clear();
    }


    @ApiStatus.Internal
    public void loadControlled() {
        final var file = new File(getDataFolder(), "controlled.json");
        if (!file.exists()) {
            return;
        }

        final var gson = new GsonBuilder().setLenient()
                                          .disableHtmlEscaping()
                                          .enableComplexMapKeySerialization()
                                          .create();

        try (final var reader = new FileReader(file)) {
            //noinspection UnstableApiUsage
            final Map<String, Long> data = gson.fromJson(reader, TOKEN.getType());
            if (data == null || data.isEmpty()) {
                return;
            }

            this.controlled.putAll(data);
        } catch (final Throwable ex) {
            getLogger().log(Level.SEVERE, "failed to load controlled data from file [" + file + "]", ex);
        }
    }

    @ApiStatus.Internal
    public void saveControlled() {
        final var file = new File(getDataFolder(), "controlled.json");

        final var gson = new GsonBuilder().setLenient()
                                          .disableHtmlEscaping()
                                          .enableComplexMapKeySerialization()
                                          .create();

        try (final var writer = new FileWriter(file)) {
            //noinspection UnstableApiUsage
            gson.toJson(this.controlled, TOKEN.getType(), writer);
        } catch (final Throwable ex) {
            getLogger().log(Level.SEVERE, "failed to save controlled data into file [" + file + "]", ex);
        }
    }


    @ApiStatus.Internal
    public @NotNull OptionalLong getControllerFlagMask(@NotNull final String data) {
        final var mask = this.controlled.getOrDefault(data, Long.MIN_VALUE);

        if (mask == Long.MIN_VALUE) {
            return OptionalLong.empty();
        } else {
            return OptionalLong.of(mask);
        }
    }

    @ApiStatus.Internal
    public @NotNull OptionalLong setControllerFlagMask(@NotNull final String data, final long mask) {
        final var prev = getControllerFlagMask(data);

        if (mask == Long.MIN_VALUE) {
            this.controlled.removeLong(data);
        } else {
            this.controlled.put(data, mask);
        }

        return prev;
    }


    public @NotNull OptionalLong getControllerFlagMask(@NotNull final Material type) {
        return getControllerFlagMask(type.key().asString());
    }

    public @NotNull OptionalLong getControllerFlagMask(@NotNull final BlockData data) {
        return getControllerFlagMask(data.getAsString(true));
    }

    public @NotNull OptionalLong getControllerFlagMask(@NotNull final ItemStack item) {
        return getControllerFlagMask(item.getType());
    }


    public @NotNull OptionalLong setControllerFlagMask(@NotNull final Material type, final long mask) {
        return setControllerFlagMask(type.key().asString(), mask);
    }

    public @NotNull OptionalLong setControllerFlagMask(@NotNull final BlockData data, final long mask) {
        return setControllerFlagMask(data.getAsString(true), mask);
    }

    public @NotNull OptionalLong setControllerFlagMask(@NotNull final ItemStack item, final long mask) {
        return setControllerFlagMask(item.getType(), mask);
    }


    public boolean test(@NotNull final Permissible target, @NotNull final ControllerFlag flag, @NotNull final OptionalLong mask) {
        if (mask.isEmpty()) {
            return false;
        }

        if (!flag.test(mask.getAsLong())) {
            return false;
        }

        if (target.hasPermission("micro.controller.bypass")) {
            return false;
        }

        if (target.hasPermission("micro.controller.bypass." + flag.name().toLowerCase(Locale.ROOT))) {
            return false;
        }

        return true;
    }


    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onClick(@NotNull final PlayerInteractEvent event) {
        final var item = event.getItem();
        if (item != null) {
            if (test(event.getPlayer(), ControllerFlag.CLICK, getControllerFlagMask(item))) {
                event.setCancelled(true);
                return;
            }
        }

        final var block = event.getClickedBlock();
        if (block != null) {
            if (test(event.getPlayer(), ControllerFlag.CLICK, getControllerFlagMask(block.getType()))) {
                event.setCancelled(true);
                return;
            }

            if (test(event.getPlayer(), ControllerFlag.CLICK, getControllerFlagMask(block.getBlockData()))) {
                event.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onBreak(@NotNull final BlockBreakEvent event) {
        if (test(event.getPlayer(), ControllerFlag.BREAK, getControllerFlagMask(event.getBlock().getType()))) {
            event.setCancelled(true);
            return;
        }

        if (test(event.getPlayer(), ControllerFlag.BREAK, getControllerFlagMask(event.getBlock().getBlockData()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onPlace(@NotNull final BlockPlaceEvent event) {
        if (test(event.getPlayer(), ControllerFlag.PLACE, getControllerFlagMask(event.getBlock().getType()))) {
            event.setCancelled(true);
            return;
        }

        if (test(event.getPlayer(), ControllerFlag.PLACE, getControllerFlagMask(event.getBlockPlaced().getBlockData()))) {
            event.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onPick(@NotNull final PlayerAttemptPickupItemEvent event) {
        if (test(event.getPlayer(), ControllerFlag.PICK, getControllerFlagMask(event.getItem().getItemStack()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    private void onDrop(@NotNull final PlayerDropItemEvent event) {
        if (test(event.getPlayer(), ControllerFlag.DROP, getControllerFlagMask(event.getItemDrop().getItemStack()))) {
            event.setCancelled(true);
        }
    }

}
