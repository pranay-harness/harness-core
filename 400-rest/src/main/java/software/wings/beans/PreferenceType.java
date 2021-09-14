/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
