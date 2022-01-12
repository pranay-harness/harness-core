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
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
public class NGBeanModule extends AbstractModule {
  private static NGBeanModule instance;

  public static NGBeanModule getInstance() {
    if (instance == null) {
      instance = new NGBeanModule();
    }
    return instance;
  }

  @Override
  protected void configure() {}
}
