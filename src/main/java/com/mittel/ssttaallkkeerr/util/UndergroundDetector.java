package com.mittel.ssttaallkkeerr.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public class UndergroundDetector {

    /**
     * プレイヤーが地下（洞窟など）にいるかどうかを判定する。
     * 条件：
     * 1. 空が見えないこと（屋根や地面がある）
     * 2. Y座標が海抜(62)より低いこと（目安としてY=55以下を地下とする）
     */
    public static boolean isPlayerUnderground(ServerLevel level, Player player) {
        BlockPos pos = player.blockPosition();

        // 1. Y座標が地下であるか（55以下）
        if (pos.getY() > 55) {
            return false;
        }

        // 2. 空が見えないか（上に遮るものがあるか）
        if (level.canSeeSky(pos)) {
            return false;
        }

        return true;
    }
}
