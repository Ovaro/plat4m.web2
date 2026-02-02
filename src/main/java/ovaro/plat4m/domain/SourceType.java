package ovaro.plat4m.domain;

public enum SourceType {
    LOCAL(0),
    MS_MONEY(1),
    UNKNOWN(-1);

    private final int sourceType;

    SourceType(int sourceType) {
        this.sourceType = sourceType;
    }

    public int getValue() {
        return this.sourceType;
    }

    public static SourceType toSourceType(int sourceType) {
        switch (sourceType) {
            case 0:
                return LOCAL;
            case 1:
                return MS_MONEY;
            default:
                return UNKNOWN;
        }
    }
}
