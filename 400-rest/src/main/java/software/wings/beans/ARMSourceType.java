package software.wings.beans;

public enum ARMSourceType {
  TEMPLATE_BODY("Template Body"),
  GIT("Git Repository"),
  UNKNOWN("Unknown");

  private final String sourceTypeLabel;
  ARMSourceType(String sourceTypeLabel) {
    this.sourceTypeLabel = sourceTypeLabel;
  }

  public static String getSourceType(String sourceType) {
    String sourceTypeLabel = UNKNOWN.sourceTypeLabel;
    for (ARMSourceType type : values()) {
      if (type.name().equalsIgnoreCase(sourceType)) {
        sourceTypeLabel = type.sourceTypeLabel;
        break;
      }
    }
    return sourceTypeLabel;
  }
}
