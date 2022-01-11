/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineServiceUtilityModule extends AbstractModule {
  private static PipelineServiceUtilityModule instance;

  public static PipelineServiceUtilityModule getInstance() {
    if (instance == null) {
      instance = new PipelineServiceUtilityModule();
    }
    return instance;
  }

  protected void configure() {}
}
