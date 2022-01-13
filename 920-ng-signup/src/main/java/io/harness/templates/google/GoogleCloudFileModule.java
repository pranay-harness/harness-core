/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.templates.google;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.templates.google.impl.GoogleCloudFileServiceImpl;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.GTM)
public class GoogleCloudFileModule extends AbstractModule {
  private static GoogleCloudFileModule instance;

  private GoogleCloudFileModule() {}

  public static GoogleCloudFileModule getInstance() {
    if (instance == null) {
      instance = new GoogleCloudFileModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(GoogleCloudFileService.class).to(GoogleCloudFileServiceImpl.class);
  }
}
