/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.opaclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;

@OwnedBy(HarnessTeam.PIPELINE)
public class OpaClientModule extends AbstractModule {
  private final String opaServiceBaseUrl;
  private final String jwtAuthSecret;

  public OpaClientModule(String opaServiceBaseUrl, String jwtAuthSecret) {
    this.opaServiceBaseUrl = opaServiceBaseUrl;
    this.jwtAuthSecret = jwtAuthSecret;
  }

  @Override
  public void configure() {
    bind(OpaServiceClient.class).toProvider(new OpaClientFactory(opaServiceBaseUrl, jwtAuthSecret));
  }
}
