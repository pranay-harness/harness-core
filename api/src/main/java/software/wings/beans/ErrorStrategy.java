/**
 *
 */

package software.wings.beans;

/**
 * The enum Error strategy.
 *
 * @author Rishi
 */
public enum ErrorStrategy {
  /**
   * Continue error strategy.
   */
  CONTINUE("Continue"),
  /**
   * Abort error strategy.
   */
  FAIL("Fail"),
  /**
   * Pause error strategy.
   */
  PAUSE("Pause"),
  /**
   * Retry error strategy.
   */
  RETRY("Retry");

  private String displayName;

  ErrorStrategy(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }
}
