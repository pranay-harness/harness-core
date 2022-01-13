/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

/**
 * The Enum ChecksumType.
 */
@OwnedBy(DEL)
public enum ChecksumType {
  /**
   * Md 5 checksum type.
   */
  MD5,
  /**
   * Sha 1 checksum type.
   */
  SHA1,
  /**
   * Sha 256 checksum type.
   */
  SHA256;
}
