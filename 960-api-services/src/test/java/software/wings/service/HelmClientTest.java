/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ANUBHAW;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.helm.HelmClient;
import io.harness.helm.HelmClientImpl;
import io.harness.helm.HelmCommandData;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by anubhaw on 3/22/18.
 */
@Slf4j
@OwnedBy(CDP)
public class HelmClientTest extends CategoryTest {
  private HelmClient helmClient = new HelmClientImpl();
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldInstall() throws InterruptedException, IOException, TimeoutException, ExecutionException {
    //    HelmCommandResponse helmCommandResponse =
    //        helmClient.install(HelmInstallCommandRequest.builder()HelmCommandTaskTest
    //                               .chartName("/Users/anubhaw/work/helm-charts/todolist")
    //                               .namespace("default")
    //                               .releaseName("rel1")
    //                               .releaseVersion("1")
    //                               .timeoutInMillis(60000)
    //                               .valueOverrides(ImmutableMap.of("image.tag", "1.12.0-alpine"))
    //                               .build());
    //    System.out.println();
    //    System.out.println();
    //    System.out.println(helmCommandResponse.getOutput());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldUpdate() throws InterruptedException, IOException, TimeoutException, ExecutionException {
    //    HelmCommandResponse helmCommandResponse =
    //        helmClient.upgrade(HelmInstallCommandRequest.builder()
    //                               .chartName("/Users/anubhaw/work/helm-charts/todolist")
    //                               .namespace("default")
    //                               .releaseName("AppName-Env-Service")
    //                               .releaseVersion("1")
    //                               .timeoutInMillis(60000)
    //                               .valueOverrides(ImmutableMap.of("image.tag", "1.13.7-alpine"))
    //                               .build());
    //    System.out.println();
    //    System.out.println();
    //    System.out.println(helmCommandResponse.getOutput());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldRollback() throws InterruptedException, IOException, TimeoutException {
    HelmClientImpl.HelmCliResponse helmCliResponse =
        helmClient.rollback(HelmCommandData.builder().releaseName("rel1").prevReleaseVersion(1).build());

    log.info(helmCliResponse.getOutput());
  }
}
