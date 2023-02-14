package com.alexsobiek.pluginlib.util;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

public class PermUtil {
    public static Optional<Group> getHighestLPGroup(LuckPerms lp, Player player, Predicate<Group> filter) {
        return Optional.ofNullable(lp.getUserManager().getUser(player.getUniqueId()))
                .flatMap(user -> Optional.of(user.getPrimaryGroup()).map(lp.getGroupManager()::getGroup).map(primary -> {
                    if (filter.test(primary)) return primary;
                    else return user.getInheritedGroups(QueryOptions.nonContextual()).stream()
                            .filter(filter)
                            .max(Comparator.comparingInt(o -> o.getWeight().orElse(0))).orElse(null);
                }));
    }
}
