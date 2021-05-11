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
