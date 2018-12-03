package io.harness.seeddata;

import static io.harness.seeddata.SeedDataProviderConstants.KUBE_PROD_ENVIRONMENT;
import static io.harness.seeddata.SeedDataProviderConstants.KUBE_QA_ENVIRONMENT;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Environment;
import software.wings.service.intfc.EnvironmentService;

@Singleton
public class EnvironmentSeedDataProvider {
  @Inject private EnvironmentService environmentService;

  public Environment createQAEnvironment(String appId) {
    return environmentService.save(
        anEnvironment().withAppId(appId).withEnvironmentType(NON_PROD).withName(KUBE_QA_ENVIRONMENT).build());
  }

  public Environment createProdEnvironment(String appId) {
    return environmentService.save(
        anEnvironment().withAppId(appId).withEnvironmentType(PROD).withName(KUBE_PROD_ENVIRONMENT).build());
  }
}
