package software.wings.beans;

/**
 * Defines readpref for mongo reads.
 *
 * @author Rishi
 */
public enum ReadPref {
  /**
   * Normal read pref.
   */
  NORMAL, /**
           * Critical read pref.
           */
  CRITICAL;
}
