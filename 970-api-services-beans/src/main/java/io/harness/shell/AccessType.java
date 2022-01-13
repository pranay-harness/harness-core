/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.shell;

/**
 * The Enum AccessType.
 */
public enum AccessType {
  /**
   * User password access type.
   */
  USER_PASSWORD,
  /**
   * User password su app user access type.
   */
  USER_PASSWORD_SU_APP_USER,
  /**
   * User password sudo app user access type.
   */
  USER_PASSWORD_SUDO_APP_USER,
  /**
   * Key access type.
   */
  KEY,
  /**
   * Key su app user access type.
   */
  KEY_SU_APP_USER,
  /**
   * Key sudo app user access type.
   */
  KEY_SUDO_APP_USER,
  /**
   * Kerberos Access Type.
   */
  KERBEROS
}
