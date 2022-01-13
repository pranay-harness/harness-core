/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.shell;

/**
 * The Enum ExecutorType.
 */
public enum ExecutorType {
  /**
   * Password auth executor type.
   */
  PASSWORD_AUTH,
  /**
   * Key auth executor type.
   */
  KEY_AUTH,
  /**
   * Bastion host executor type.
   */
  BASTION_HOST
}
