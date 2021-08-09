package software.wings.beans;

/**
 * The enum for Preference types.
 *
 */

public enum PreferenceType {
  /**
   * Deployment Preference Type.
   */
  DEPLOYMENT_PREFERENCE("Deployment Preference"),

  AUDIT_PREFERENCE("AUDIT_PREFERENCE");

  String displayName;
  PreferenceType(String displayName) {
    this.displayName = displayName;
  }
}
