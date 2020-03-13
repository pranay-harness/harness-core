package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.CCMPerpetualTaskManager;
import io.harness.ccm.CCMSettingService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.settings.SettingValue.SettingVariableTypes;

public class ClusterRecordHandlerTest extends CategoryTest {
  @Mock CCMSettingService ccmSettingService;
  @Mock ClusterRecordService clusterRecordService;
  @Mock CCMPerpetualTaskManager ccmPerpetualTaskManager;
  @InjectMocks @Spy private ClusterRecordHandler handler;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";

  private SettingAttribute prevSettingAttribute;
  private SettingAttribute settingAttribute;
  private InfrastructureDefinition infrastructureDefinition;
  private InfrastructureMapping infrastructureMapping;

  @Before
  public void setUp() {
    KubernetesClusterConfig kubernetesClusterConfig = new KubernetesClusterConfig();
    kubernetesClusterConfig.setType(SettingVariableTypes.KUBERNETES_CLUSTER.name());

    prevSettingAttribute = aSettingAttribute()
                               .withUuid(cloudProviderId)
                               .withAccountId(accountId)
                               .withName("PREV_NAME")
                               .withValue(kubernetesClusterConfig)
                               .build();

    settingAttribute = aSettingAttribute()
                           .withUuid(cloudProviderId)
                           .withAccountId(accountId)
                           .withName("CURR_NAME")
                           .withValue(kubernetesClusterConfig)
                           .build();

    infrastructureDefinition =
        InfrastructureDefinition.builder().infrastructure(DirectKubernetesInfrastructure.builder().build()).build();

    infrastructureMapping = DirectKubernetesInfrastructureMapping.builder()
                                .accountId(accountId)
                                .infraMappingType(DIRECT_KUBERNETES)
                                .build();

    ClusterRecord clusterRecord = ClusterRecord.builder().build();

    when(ccmSettingService.isCloudCostEnabled(isA(SettingAttribute.class))).thenReturn(true);
    when(clusterRecordService.from(isA(SettingAttribute.class))).thenReturn(clusterRecord);
    when(clusterRecordService.from(isA(InfrastructureDefinition.class))).thenReturn(clusterRecord);
    when(clusterRecordService.from(isA(InfrastructureMapping.class))).thenReturn(clusterRecord);

    when(clusterRecordService.upsert(isA(ClusterRecord.class))).thenReturn(clusterRecord);
    when(clusterRecordService.delete(anyString(), anyString())).thenReturn(true);

    when(ccmPerpetualTaskManager.resetPerpetualTasks(isA(SettingAttribute.class))).thenReturn(true);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldUpsertOnSavedCloudProvider() {
    handler.onSaved(settingAttribute);
    verify(clusterRecordService).upsert(isA(ClusterRecord.class));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)

  public void shouldUpsertOnUpdatedCloudProvider() {
    handler.onUpdated(prevSettingAttribute, settingAttribute);
    verify(clusterRecordService).upsert(isA(ClusterRecord.class));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testOnDeletedCloudProvider() {
    handler.onDeleted(settingAttribute);
    verify(clusterRecordService).deactivate(accountId, cloudProviderId);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testOnSavedInfrastructureDefinition() {
    handler.onSaved(infrastructureDefinition);
    verify(clusterRecordService).upsert(isA(ClusterRecord.class));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testOnUpdatedInfrastructureDefinition() {
    handler.onUpdated(infrastructureDefinition);
    verify(clusterRecordService).upsert(isA(ClusterRecord.class));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testOnSavedInfrastructureMapping() {
    handler.onSaved(infrastructureMapping);
    verify(clusterRecordService).upsert(isA(ClusterRecord.class));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testOnUpdatedInfrastructureMapping() {
    handler.onUpdated(infrastructureMapping);
    verify(clusterRecordService).upsert(isA(ClusterRecord.class));
  }
}
