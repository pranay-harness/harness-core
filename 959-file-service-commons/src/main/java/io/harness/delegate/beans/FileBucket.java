/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._957_CG_BEANS)
public enum FileBucket {
  LOB,
  ARTIFACTS,
  AUDITS,
  CONFIGS,
  CUSTOM_MANIFEST,
  LOGS,
  PLATFORMS,
  TERRAFORM_STATE,
  PROFILE_RESULTS,
  TERRAFORM_PLAN,
  EXPORT_EXECUTIONS;

  private int chunkSize;

  /**
   * Instantiates a new file bucket.
   *
   * @param chunkSize the chunk size
   */
  FileBucket(int chunkSize) {
    this.chunkSize = chunkSize;
  }

  /**
   * Instantiates a new file bucket.
   */
  FileBucket() {
    this(1000 * 1000);
  }

  public String representationName() {
    return name().toLowerCase();
  }

  /**
   * Gets chunk size.
   *
   * @return the chunk size
   */
  public int getChunkSize() {
    return chunkSize;
  }
}
