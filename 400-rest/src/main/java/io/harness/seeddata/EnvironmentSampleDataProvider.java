/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.seeddata;

import static io.harness.beans.EnvironmentType.NON_PROD;
import static io.harness.beans.EnvironmentType.PROD;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_PROD_ENVIRONMENT;
import static io.harness.seeddata.SampleDataProviderConstants.K8S_QA_ENVIRONMENT;

import static software.wings.beans.Environment.Builder.anEnvironment;

import software.wings.beans.Environment;
import software.wings.service.intfc.EnvironmentService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class EnvironmentSampleDataProvider {
  @Inject private EnvironmentService environmentService;

  public Environment createQAEnvironment(String appId) {
    return environmentService.save(
        anEnvironment().appId(appId).environmentType(NON_PROD).name(K8S_QA_ENVIRONMENT).sample(true).build());
  }

  public Environment createProdEnvironment(String appId) {
    return environmentService.save(
        anEnvironment().appId(appId).environmentType(PROD).name(K8S_PROD_ENVIRONMENT).sample(true).build());
  }
}
