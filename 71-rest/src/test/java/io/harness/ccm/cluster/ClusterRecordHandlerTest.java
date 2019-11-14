package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;

public class ClusterRecordHandlerTest extends WingsBaseTest {
  @Mock ClusterRecordService clusterRecordService;
  @Inject @InjectMocks @Spy private ClusterRecordHandler handler;

  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";

  private SettingAttribute settingAttribute;
  private InfrastructureMapping infrastructureMapping;

  @Before
  public void setUp() {
    settingAttribute = aSettingAttribute()
                           .withUuid(cloudProviderId)
                           .withAccountId(accountId)
                           .withValue(StringValue.Builder.aStringValue().build())
                           .build();
    infrastructureMapping = DirectKubernetesInfrastructureMapping.builder()
                                .accountId(accountId)
                                .infraMappingType(DIRECT_KUBERNETES)
                                .build();
    ClusterRecord clusterRecord = ClusterRecord.builder().build();
    when(clusterRecordService.upsert(isA(ClusterRecord.class))).thenReturn(clusterRecord);
    when(clusterRecordService.delete(anyString(), anyString())).thenReturn(true);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testOnSaved() {
    handler.onSaved(infrastructureMapping);
    verify(clusterRecordService).upsert(isA(ClusterRecord.class));
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testOnUpdated() {
    handler.onUpdated(infrastructureMapping);
    verify(clusterRecordService).upsert(isA(ClusterRecord.class));
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testOnDeleted() {
    handler.onDeleted(settingAttribute);
    verify(clusterRecordService).delete(accountId, cloudProviderId);
  }
}
