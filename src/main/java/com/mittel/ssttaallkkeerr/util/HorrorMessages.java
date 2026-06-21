package com.mittel.ssttaallkkeerr.util;

import net.minecraft.util.RandomSource;

public class HorrorMessages {

    private static final String[] MESSAGES = {
        "I SEE YOU",
        "LOOK BEHIND YOU",
        "WHY DID YOU RUN?",
        "YOU CANNOT HIDE",
        "LET ME IN",
        "IT IS COLD OUT HERE",
        "WHO ARE YOU?",
        "I AM ALWAYS HERE",
        "DO NOT TURN AROUND",
        "PLEASE WAKE UP",
        "HE IS COMING",
        "RUN",
        "I FOUND YOUR HOUSE",
        "ARE YOU ALONE?",
        "I AM HUNGRY",
        "THIS IS MY WORLD",
        "LEAVE NOW",
        "THEY ARE WATCHING",
        "DON'T LOOK OUT THE WINDOW",
        "I MISS YOU"
    };

    /**
     * ランダムなホラーメッセージを取得します。
     */
    public static String getRandomMessage(RandomSource random) {
        return MESSAGES[random.nextInt(MESSAGES.length)];
    }
}
