package software.wings.sm;

/**
 * Describes what type of element is being repeated on.
 *
 * @author Rishi
 */
public enum RepeatElementType {
  SERVICE("service"),
  PHASE("phase"),
  DATA_CENTER("dataCenter"),
  OZ("operationalZone"),
  HOST("host"),
  OTHER("");

  private String displayName;

  RepeatElementType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}
