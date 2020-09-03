package io.harness.functional.harnesscli;

import static io.harness.generator.ServiceGenerator.Services.K8S_V2_TEST;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

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
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.artifact.ArtifactStream;

import java.io.IOException;
import java.util.List;

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
  public void getArtifactsTest() throws IOException {
    String command = "harness get artifacts --service " + serviceId;
    List<String> getArtifactsOutput = harnesscliHelper.executeCLICommand(command);
    assertThat(getArtifactsOutput.get(1)).isEqualTo(artifactStream.getName());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(CliFunctionalTests.class)
  public void getArtifactsWithWrongServiceIdTest() throws IOException {
    String command = "harness get artifacts --service "
        + "wrongServiceId";
    List<String> getArtifactsOutput = harnesscliHelper.executeCLICommand(command);

    assertThat(getArtifactsOutput.get(0).contains("No Artifacts were found in the Application Service provided"))
        .isTrue();
  }
}
