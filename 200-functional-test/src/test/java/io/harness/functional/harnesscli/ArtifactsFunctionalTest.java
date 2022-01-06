/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.harnesscli;

import static io.harness.generator.ServiceGenerator.Services.K8S_V2_TEST;
import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.artifactstream.DockerArtifactStreamStreamsGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ArtifactRestUtils;

import software.wings.beans.artifact.ArtifactStream;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class ArtifactsFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private DockerArtifactStreamStreamsGenerator dockerArtifactStreamStreamsGenerator;
  @Inject HarnesscliHelper harnesscliHelper;

  final Seed seed = new Seed(0);
  private Owners owners;
  private String serviceId;
  private ArtifactStream artifactStream;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    serviceGenerator.ensurePredefined(seed, owners, K8S_V2_TEST);
    artifactStream = dockerArtifactStreamStreamsGenerator.ensureArtifactStream(seed, owners);

    ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, artifactStream.getAppId(), artifactStream.getUuid(), 0);
    ArtifactRestUtils.fetchArtifactByArtifactStream(bearerToken, artifactStream.getAppId(), artifactStream.getUuid());

    serviceId = artifactStream.getServiceId();
    harnesscliHelper.loginToCLI();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void getArtifactsTest() throws IOException {
    String command = "harness get artifacts --service " + serviceId;
    List<String> getArtifactsOutput = harnesscliHelper.executeCLICommand(command);
    assertThat(getArtifactsOutput.get(1)).isEqualTo(artifactStream.getName());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void getArtifactsWithWrongServiceIdTest() throws IOException {
    String command = "harness get artifacts --service "
        + "wrongServiceId";
    List<String> getArtifactsOutput = harnesscliHelper.executeCLICommand(command);

    assertThat(getArtifactsOutput.get(0).contains("No Artifacts were found in the Application Service provided"))
        .isTrue();
  }
}
