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
  CONTINUE("Continue"), /**
                         * Abort error strategy.
                         */
  ABORT("Abort"), /**
                   * Pause error strategy.
                   */
  PAUSE("Pause"), /**
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

  /**
   * Sets display name.
   *
   * @param displayName the display name
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}
