package io.harness.ngtriggers.helpers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.NGTriggerType.ARTIFACT;
import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
import io.harness.ngtriggers.beans.source.artifact.ArtifactTriggerConfig;
import io.harness.ngtriggers.beans.source.artifact.ArtifactType;
import io.harness.ngtriggers.beans.source.artifact.EcrSpec;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.polling.contracts.DockerHubPayload;
import io.harness.polling.contracts.EcrPayload;
import io.harness.polling.contracts.GcrPayload;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.contracts.Qualifier;
import io.harness.rule.Owner;

import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class BuildTriggerHelperTest extends CategoryTest {
  @InjectMocks BuildTriggerHelper buildTriggerHelper;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_DockerRegistry() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setDockerHubPayload(DockerHubPayload.newBuilder().setImagePath("test").build())
            .build());

    validatePollingItem(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setDockerHubPayload(DockerHubPayload.newBuilder().setImagePath("test").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoImage = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setDockerHubPayload(DockerHubPayload.newBuilder().build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoImage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("imagePath can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_Gcr() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setGcrPayload(GcrPayload.newBuilder().setRegistryHostname("gcr.io").setImagePath("test").build())
            .build());

    validatePollingItem(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setGcrPayload(GcrPayload.newBuilder().setRegistryHostname("gcr.io").setImagePath("test").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoImage = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setGcrPayload(GcrPayload.newBuilder().setRegistryHostname("gcr.io").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoImage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("imagePath can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRegistryHostName =
        generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
            PollingPayloadData.newBuilder()
                .setConnectorRef("conn")
                .setGcrPayload(GcrPayload.newBuilder().setImagePath("test").build())
                .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRegistryHostName))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("registryHostname can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testValidatePollingItemForArtifact_ecr() {
    PollingItem pollingItem = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setEcrPayload(EcrPayload.newBuilder().setRegion("us-east-1").setImagePath("test").build())
            .build());

    validatePollingItem(pollingItem);

    final PollingItem pollingItemNoConn = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setEcrPayload(EcrPayload.newBuilder().setRegion("us-east-1").setImagePath("test").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoConn))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("ConnectorRef can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoImage = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setEcrPayload(EcrPayload.newBuilder().setRegion("us-east-1").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoImage))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("imagePath can not be blank. Needs to have concrete value");

    final PollingItem pollingItemNoRegion = generatePollingItem(io.harness.polling.contracts.Category.ARTIFACT,
        PollingPayloadData.newBuilder()
            .setConnectorRef("conn")
            .setEcrPayload(EcrPayload.newBuilder().setImagePath("test").build())
            .build());

    assertThatThrownBy(() -> buildTriggerHelper.validatePollingItemForArtifact(pollingItemNoRegion))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("region can not be blank. Needs to have concrete value");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testVerifyStageAndBuildRef() {
    ArtifactTriggerConfig ecr =
        ArtifactTriggerConfig.builder().type(ArtifactType.ECR).spec(EcrSpec.builder().build()).build();
    ecr.setStageIdentifier("dev");
    ecr.setArtifactRef("primary");
    NGTriggerConfigV2 ngTriggerConfigV2 =
        NGTriggerConfigV2.builder().source(NGTriggerSourceV2.builder().type(ARTIFACT).spec(ecr).build()).build();

    try {
      buildTriggerHelper.verifyStageAndBuildRef(
          TriggerDetails.builder().ngTriggerConfigV2(ngTriggerConfigV2).build(), "artifactRef");
    } catch (Exception e) {
      fail("Exception was n ot expected");
    }

    ecr.setStageIdentifier(null);
    assertThatThrownBy(()
                           -> buildTriggerHelper.verifyStageAndBuildRef(
                               TriggerDetails.builder().ngTriggerConfigV2(ngTriggerConfigV2).build(), "artifactRef"))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("stageIdentifier can not be blank/missing. ");

    ecr.setArtifactRef(null);
    assertThatThrownBy(()
                           -> buildTriggerHelper.verifyStageAndBuildRef(
                               TriggerDetails.builder().ngTriggerConfigV2(ngTriggerConfigV2).build(), "artifactRef"))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("stageIdentifier can not be blank/missing. artifactRef can not be blank/missing. ");
  }

  private void validatePollingItem(PollingItem pollingItem) {
    try {
      buildTriggerHelper.validatePollingItemForArtifact(pollingItem);
    } catch (Exception e) {
      fail("Exception wasnt expected");
    }
  }

  PollingItem generatePollingItem(
      io.harness.polling.contracts.Category category, PollingPayloadData pollingPayloadData) {
    return PollingItem.newBuilder()
        .setCategory(category)
        .setQualifier(Qualifier.newBuilder().setProjectId("proj").setOrganizationId("org").setAccountId("acc").build())
        .setSignature("sig1")
        .setPollingPayloadData(pollingPayloadData)
        .build();
  }
}
