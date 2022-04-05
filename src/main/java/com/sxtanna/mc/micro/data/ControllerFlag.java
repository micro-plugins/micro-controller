package com.sxtanna.mc.micro.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.EnumSet;
import java.util.Set;

public enum ControllerFlag {

    CLICK("Interact"),

    BREAK("Break Block"),
    PLACE("Place Block"),

    PICK("Pickup Item"),
    DROP("Drop Item");


    @NonNull
    private static final Set<ControllerFlag> VALUES = Set.copyOf(EnumSet.allOf(ControllerFlag.class));


    @Contract(pure = true)
    public static long pack(@NotNull @Unmodifiable final Set<ControllerFlag> perms) {
        var result = 0L;

        for (final var perm : perms) {
            result |= perm.mask;
        }

        return result;
    }

    public static @NotNull @Unmodifiable Set<ControllerFlag> read(final long value) {
        final var result = EnumSet.noneOf(ControllerFlag.class);

        for (final var perm : VALUES) {
            if ((value & perm.mask) != 0) {
                result.add(perm);
            }
        }

        return Set.copyOf(result);
    }


    public final long   mask;
    @NotNull
    public final String disp;


    @Contract(pure = true)
    ControllerFlag(@NotNull final String disp) {
        this.disp = disp;
        this.mask = 1L << this.ordinal();
    }


    @Contract(pure = true)
    public boolean test(final long value) {
        return (value & this.mask) != 0;
    }

}
