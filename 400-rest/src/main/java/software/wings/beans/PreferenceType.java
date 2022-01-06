/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
