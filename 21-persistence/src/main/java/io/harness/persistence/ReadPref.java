package io.harness.persistence;

/**
 * Defines readpref for mongo reads.
 */
public enum ReadPref {
  /**
   * Normal read pref.
   */
  NORMAL,
  /**
   * Critical read pref.
   */
  CRITICAL;
}
