/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
