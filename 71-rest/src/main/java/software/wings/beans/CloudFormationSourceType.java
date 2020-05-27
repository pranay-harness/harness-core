package software.wings.beans;

public enum CloudFormationSourceType {
  TEMPLATE_BODY("Template Body"),
  TEMPLATE_URL("Amazon S3"),
  GIT("Git Repository"),
  UNKNOWN("Unknown");

  private final String sourceTypeLabel;

  CloudFormationSourceType(String sourceTypeLabel) {
    this.sourceTypeLabel = sourceTypeLabel;
  }

  public static String getSourceType(String sourceType) {
    String sourceTypeLabel = UNKNOWN.sourceTypeLabel;
    for (CloudFormationSourceType type : values()) {
      if (type.name().equalsIgnoreCase(sourceType)) {
        sourceTypeLabel = type.sourceTypeLabel;
        break;
      }
    }
    return sourceTypeLabel;
  }
}