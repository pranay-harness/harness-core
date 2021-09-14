/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.jobs;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.activity.source.services.api.KubernetesActivitySourceService;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.services.api.DataCollectionTaskService;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.encryption.Scope;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.StringValue;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ConnectorChangeEventMessageProcessorTest extends CvNextGenTestBase {
  @Inject private ConnectorChangeEventMessageProcessor connectorChangeEventMessageProcessor;
  @Mock private DataCollectionTaskService dataCollectionTaskService;
  @Mock private KubernetesActivitySourceService kubernetesActivitySourceService;
  @Inject private ActivitySourceService activitySourceService;
  @Inject private MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService;
  @Inject private HPersistence hPersistence;

  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String connectorIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    FieldUtils.writeField(
        connectorChangeEventMessageProcessor, "dataCollectionTaskService", dataCollectionTaskService, true);
    FieldUtils.writeField(
        connectorChangeEventMessageProcessor, "kubernetesActivitySourceService", kubernetesActivitySourceService, true);
    accountIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    connectorIdentifier = "connectorIdentifier";
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProcessUpdateAction_resetLiveMonitoringPerpetualTask() throws IllegalAccessException {
    MonitoringSourcePerpetualTaskService monitoringSourcePerpetualTaskService =
        Mockito.mock(MonitoringSourcePerpetualTaskService.class);
    MonitoringSourcePerpetualTask monitoringSourcePerpetualTask = MonitoringSourcePerpetualTask.builder()
                                                                      .accountId(accountIdentifier)
                                                                      .orgIdentifier(orgIdentifier)
                                                                      .projectIdentifier(projectIdentifier)
                                                                      .connectorIdentifier(connectorIdentifier)
                                                                      .monitoringSourceIdentifier(generateUuid())
                                                                      .perpetualTaskId(generateUuid())
                                                                      .build();
    when(monitoringSourcePerpetualTaskService.listByConnectorIdentifier(
             accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, Scope.PROJECT))
        .thenReturn(Lists.newArrayList(monitoringSourcePerpetualTask));

    FieldUtils.writeField(connectorChangeEventMessageProcessor, "monitoringSourcePerpetualTaskService",
        monitoringSourcePerpetualTaskService, true);
    connectorChangeEventMessageProcessor.processUpdateAction(
        EntityChangeDTO.newBuilder()
            .setAccountIdentifier(StringValue.newBuilder().setValue(accountIdentifier).build())
            .setOrgIdentifier(StringValue.newBuilder().setValue(orgIdentifier).build())
            .setProjectIdentifier(StringValue.newBuilder().setValue(projectIdentifier).build())
            .setIdentifier(StringValue.newBuilder().setValue(connectorIdentifier).build())
            .build());
    verify(monitoringSourcePerpetualTaskService, times(1))
        .resetLiveMonitoringPerpetualTask(monitoringSourcePerpetualTask);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessUpdateAction_resetLiveMonitoringPerpetualTaskForKubernetesActivitySource() {
    KubernetesActivitySourceDTO kubernetesActivitySourceDTO = createKubernetesActivitySourceDTO();

    String kubernetesSourceId = activitySourceService.create(accountIdentifier, kubernetesActivitySourceDTO);

    KubernetesActivitySource kubernetesActivitySource =
        (KubernetesActivitySource) activitySourceService.getActivitySource(kubernetesSourceId);

    assertThat(kubernetesActivitySource).isNotNull();

    connectorChangeEventMessageProcessor.processUpdateAction(
        EntityChangeDTO.newBuilder()
            .setAccountIdentifier(StringValue.newBuilder().setValue(accountIdentifier).build())
            .setOrgIdentifier(StringValue.newBuilder().setValue(orgIdentifier).build())
            .setProjectIdentifier(StringValue.newBuilder().setValue(projectIdentifier).build())
            .setIdentifier(StringValue.newBuilder().setValue(connectorIdentifier).build())
            .build());

    verify(kubernetesActivitySourceService, times(1))
        .findByConnectorIdentifier(
            accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, Scope.PROJECT);
  }

  private KubernetesActivitySourceDTO createKubernetesActivitySourceDTO() {
    return KubernetesActivitySourceDTO.builder()
        .identifier(generateUuid())
        .name("some-name")
        .connectorIdentifier(connectorIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .activitySourceConfigs(Sets.newHashSet(KubernetesActivitySourceConfig.builder()
                                                   .serviceIdentifier(generateUuid())
                                                   .envIdentifier(generateUuid())
                                                   .namespace(generateUuid())
                                                   .workloadName(generateUuid())
                                                   .build()))
        .build();
  }
}
