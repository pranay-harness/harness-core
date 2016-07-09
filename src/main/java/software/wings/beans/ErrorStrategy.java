/**
 *
 */
package software.wings.beans;

/**
 * @author Rishi
 *
 */
public enum ErrorStrategy {
  CONTINUE("Continue"),
  ABORT("Abort"),
  PAUSE("Pause"),
  RETRY("Retry");

  private String displayName;

  ErrorStrategy(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}
