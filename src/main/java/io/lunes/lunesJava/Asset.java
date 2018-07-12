package io.lunes.lunesJava;

public abstract class Asset {
    /**
     * Constant used to represent LUNES token in asset transactions.
     */
    public static final String LUNES = "LUNES";

    public static final long TOKEN = 100000000L;

    public static final long MILLI = 100000L;

    static String normalize(String assetId) {
        return assetId == null || assetId.isEmpty() ? Asset.LUNES : assetId;
    }

    static boolean isLunes(String assetId) {
        return LUNES.equals(normalize(assetId));
    }

    static String toJsonObject(String assetId) {
        return isLunes(assetId) ? null : assetId;
    }
}
