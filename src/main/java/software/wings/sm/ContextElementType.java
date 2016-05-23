package software.wings.sm;

/**
 * Describes what type of element is being repeated on.
 *
 * @author Rishi
 */
public enum ContextElementType {
  SERVICE("service"),
  TAG("tag"),
  HOST("host"),
  OTHER("");

  private String displayName;

  ContextElementType(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}
