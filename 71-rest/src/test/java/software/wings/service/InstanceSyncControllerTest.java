package software.wings.service;

import static io.harness.rule.OwnerRule.AMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.FeatureName.MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK;
import static software.wings.beans.FeatureName.STOP_PROCESSING_INSTANCE_SYNC_FROM_ITERATOR_PCF_DEPLOYMENTS;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.api.PcfDeploymentInfo;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.service.impl.instance.PcfInstanceHandler;

import java.util.Arrays;

public class InstanceSyncControllerTest extends WingsBaseTest {
  public static final String ACCOUNT_ID = "accountId";

  @Inject InstanceSyncController instanceSyncController;

  @Before
  public void setUp() throws Exception {}

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testCanUpdateDbForPcfDeployments() {
    PcfInfrastructureMapping pcfInfrastructureMapping = PcfInfrastructureMapping.builder()
                                                            .deploymentType("PCF_PCF")
                                                            .infraMappingType("PCF_PCF")
                                                            .accountId(ACCOUNT_ID)
                                                            .build();

    disableFeatureFlag(MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK);
    // in case of new deployment, it should always update db
    boolean canUpdateDb = instanceSyncController.canUpdateDb(
        InstanceSyncController.InstanceSyncFlow.NEW_DEPLOYMENT, pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(true);

    // ff disabled
    // in case of PCF deployments:
    canUpdateDb = instanceSyncController.canUpdateDb(
        InstanceSyncController.InstanceSyncFlow.PERPETUAL_TASK, pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(false);

    canUpdateDb = instanceSyncController.canUpdateDb(InstanceSyncController.InstanceSyncFlow.ITERATOR_INSTANCE_SYNC,
        pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(true);

    // ff enabled
    enableFeatureFlag(MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK);
    canUpdateDb = instanceSyncController.canUpdateDb(
        InstanceSyncController.InstanceSyncFlow.PERPETUAL_TASK, pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(true);

    canUpdateDb = instanceSyncController.canUpdateDb(InstanceSyncController.InstanceSyncFlow.ITERATOR_INSTANCE_SYNC,
        pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(false);

    canUpdateDb = instanceSyncController.canUpdateDb(
        InstanceSyncController.InstanceSyncFlow.NEW_DEPLOYMENT, pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(true);
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testCanUpdateDbForNonPcfDeployments() {
    PcfInfrastructureMapping pcfInfrastructureMapping = PcfInfrastructureMapping.builder()
                                                            .deploymentType("AZURE_KUBERNETES")
                                                            .infraMappingType("AZURE_KUBERNETES")
                                                            .accountId(ACCOUNT_ID)
                                                            .build();

    // in case of new deployment, it should always update db
    disableFeatureFlag(MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK);
    boolean canUpdateDb = instanceSyncController.canUpdateDb(
        InstanceSyncController.InstanceSyncFlow.NEW_DEPLOYMENT, pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(true);

    // ff disabled
    // in case of PCF deployments:
    canUpdateDb = instanceSyncController.canUpdateDb(
        InstanceSyncController.InstanceSyncFlow.PERPETUAL_TASK, pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(false);

    canUpdateDb = instanceSyncController.canUpdateDb(InstanceSyncController.InstanceSyncFlow.ITERATOR_INSTANCE_SYNC,
        pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(true);

    // ff enabled
    enableFeatureFlag(MOVE_PCF_INSTANCE_SYNC_TO_PERP_TASK);
    canUpdateDb = instanceSyncController.canUpdateDb(
        InstanceSyncController.InstanceSyncFlow.PERPETUAL_TASK, pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(false);

    canUpdateDb = instanceSyncController.canUpdateDb(InstanceSyncController.InstanceSyncFlow.ITERATOR_INSTANCE_SYNC,
        pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(true);

    canUpdateDb = instanceSyncController.canUpdateDb(
        InstanceSyncController.InstanceSyncFlow.NEW_DEPLOYMENT, pcfInfrastructureMapping, PcfInstanceHandler.class);
    assertThat(canUpdateDb).isEqualTo(true);
  }
  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void enablePerpetualTaskForAccount() {
    boolean perpetualTaskForAccount =
        instanceSyncController.enablePerpetualTaskForAccount(ACCOUNT_ID, InfrastructureMappingType.AWS_AMI.getName());
    assertThat(perpetualTaskForAccount).isFalse();
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void shouldSkipIteratorInstanceSync() {
    PcfInfrastructureMapping pcfInfrastructureMapping = PcfInfrastructureMapping.builder()
                                                            .deploymentType("AZURE_KUBERNETES")
                                                            .infraMappingType("AZURE_KUBERNETES")
                                                            .accountId(ACCOUNT_ID)
                                                            .build();
    disableFeatureFlag(STOP_PROCESSING_INSTANCE_SYNC_FROM_ITERATOR_PCF_DEPLOYMENTS);
    boolean shouldSkipIteratorInstanceSync =
        instanceSyncController.shouldSkipIteratorInstanceSync(pcfInfrastructureMapping);
    assertThat(shouldSkipIteratorInstanceSync).isFalse();

    enableFeatureFlag(STOP_PROCESSING_INSTANCE_SYNC_FROM_ITERATOR_PCF_DEPLOYMENTS);
    shouldSkipIteratorInstanceSync = instanceSyncController.shouldSkipIteratorInstanceSync(pcfInfrastructureMapping);
    assertThat(shouldSkipIteratorInstanceSync).isFalse();

    pcfInfrastructureMapping.setInfraMappingType("PCF_PCF");
    disableFeatureFlag(STOP_PROCESSING_INSTANCE_SYNC_FROM_ITERATOR_PCF_DEPLOYMENTS);
    shouldSkipIteratorInstanceSync = instanceSyncController.shouldSkipIteratorInstanceSync(pcfInfrastructureMapping);
    assertThat(shouldSkipIteratorInstanceSync).isFalse();

    enableFeatureFlag(STOP_PROCESSING_INSTANCE_SYNC_FROM_ITERATOR_PCF_DEPLOYMENTS);
    shouldSkipIteratorInstanceSync = instanceSyncController.shouldSkipIteratorInstanceSync(pcfInfrastructureMapping);
    assertThat(shouldSkipIteratorInstanceSync).isTrue();
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void createPerpetualTaskForNewDeployment() {
    DeploymentSummary deploymentSummary =
        DeploymentSummary.builder()
            .appId("appId")
            .accountId("accountId")
            .infraMappingId("infraId")
            .deploymentInfo(
                PcfDeploymentInfo.builder().applicationGuild("guid").applicationName("applicationName").build())
            .build();
    boolean perpetualTaskForNewDeployment =
        instanceSyncController.createPerpetualTaskForNewDeployment(InfrastructureMappingType.AWS_AMI, Arrays.asList());
    assertThat(perpetualTaskForNewDeployment).isFalse();
  }
}