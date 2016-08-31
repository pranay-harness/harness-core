package software.wings.beans.stats;

/**
 * Created by anubhaw on 8/15/16.
 */
public abstract class WingsStatistics {
  private StatisticsType type;

  /**
   * Instantiates a new Wings statistics.
   *
   * @param type the type
   */
  public WingsStatistics(StatisticsType type) {
    this.type = type;
  }

  /**
   * Gets type.
   *
   * @return the type
   */
  public StatisticsType getType() {
    return type;
  }

  /**
   * The enum Statistics type.
   */
  public enum StatisticsType {
    /**
     * Deployment statistics type.
     */
    DEPLOYMENT, /**
                 * Active releases statistics type.
                 */
    ACTIVE_RELEASES, /**
                      * Active artifacts statistics type.
                      */
    ACTIVE_ARTIFACTS, /**
                       * Application count statistics type.
                       */
    APPLICATION_COUNT, /**
                        * Deployment activities statistics type.
                        */
    DEPLOYMENT_ACTIVITIES, /**
                            * Top consumers statistics type.
                            */
    TOP_CONSUMERS,

    /**
     * Key statistics statistics type.
     */
    KEY_STATISTICS
  }
}
