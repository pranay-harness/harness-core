package io.harness.batch.processing.dao.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceEvent.EventType;
import io.harness.batch.processing.ccm.InstanceInfo;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.entities.InstanceData.InstanceDataKeys;
import io.harness.batch.processing.events.timeseries.data.CostEventData;
import io.harness.batch.processing.events.timeseries.service.intfc.CostEventService;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.instance.HarnessServiceInfo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class InstanceDataDaoImplTest extends WingsBaseTest {
  @Inject @InjectMocks private InstanceDataDaoImpl instanceDataDao;
  @Mock private CostEventService costEventService;
  @Captor private ArgumentCaptor<CostEventData> costEventDataArgumentCaptor;

  private static final String RUNNING_INSTANCE_ID = "running_instance_id";
  private static final String INSTANCE_NAME = "instance_name";
  private static final String ACCOUNT_ID = "account_id";
  private static final String CLOUD_PROVIDER_ID = "cloud_provider_id";
  private static final String CLUSTER_NAME = "cluster_name";
  private static final String CLUSTER_ID = "cluster_id";
  private final double DEFAULT_INSTANCE_CPU = 36;
  private final double DEFAULT_INSTANCE_MEMORY = 60;
  private final Instant NOW = Instant.now();
  private final Instant START_INSTANT = NOW.truncatedTo(ChronoUnit.DAYS);
  private final Instant PREV_START_INSTANT = NOW.minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
  private final Instant END_INSTANT = NOW.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnInstanceDataWithName() {
    instanceDataDao.create(instanceData(RUNNING_INSTANCE_ID, InstanceState.RUNNING));
    InstanceData instanceData =
        instanceDataDao.fetchInstanceDataWithName(ACCOUNT_ID, CLUSTER_ID, INSTANCE_NAME, START_INSTANT.toEpochMilli());
    assertThat(instanceData).isNotNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnActiveInstance() {
    instanceDataDao.create(instanceData(RUNNING_INSTANCE_ID, InstanceState.RUNNING));
    InstanceData instanceData =
        instanceDataDao.fetchActiveInstanceData(ACCOUNT_ID, CLUSTER_ID, RUNNING_INSTANCE_ID, getActiveInstanceState());
    assertThat(instanceData).isNotNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnEmptyClusterActiveInstance() {
    instanceDataDao.create(instanceData(RUNNING_INSTANCE_ID, InstanceState.RUNNING));
    List<InstanceData> instanceData = instanceDataDao.fetchClusterActiveInstanceData(
        ACCOUNT_ID, CLUSTER_ID, getActiveInstanceState(), PREV_START_INSTANT);
    assertThat(instanceData).hasSize(0);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnClusterActiveInstance() {
    instanceDataDao.create(instanceData(RUNNING_INSTANCE_ID, InstanceState.RUNNING));
    List<InstanceData> instanceData =
        instanceDataDao.fetchClusterActiveInstanceData(ACCOUNT_ID, CLUSTER_ID, getActiveInstanceState(), END_INSTANT);
    assertThat(instanceData).hasSize(1);
    assertThat(instanceData.get(0).getInstanceId()).isEqualTo(RUNNING_INSTANCE_ID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  @Ignore("TODO: Ignoring for now will enable with deployment events.")
  public void shouldUpsertInstanceInfo() {
    InstanceData instanceData = instanceDataDao.upsert(instanceInfo());
    verify(costEventService).updateDeploymentEvent(costEventDataArgumentCaptor.capture());
    CostEventData costEventData = costEventDataArgumentCaptor.getValue();
    assertThat(costEventData.getDeploymentId()).isEqualTo("deploymentSummaryId");
    assertThat(costEventData.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(costEventData.getNamespace()).isEqualTo(InstanceMetaDataConstants.NAMESPACE);
    assertThat(costEventData.getWorkloadName()).isEqualTo(InstanceMetaDataConstants.WORKLOAD_NAME);
    assertThat(costEventData.getWorkloadType()).isEqualTo(InstanceMetaDataConstants.WORKLOAD_TYPE);
    assertThat(costEventData.getSettingId()).isEqualTo(CLOUD_PROVIDER_ID);
    assertThat(instanceData.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(instanceData.getHarnessServiceInfo()).isEqualTo(harnessServiceInfo());
    assertThat(instanceData.getMetaData()).isEqualTo(metaData());
    assertThat(instanceData.getTotalResource()).isEqualTo(resource());
    assertThat(instanceData.getLabels()).isEqualTo(label());
    assertThat(instanceData.getUsageStartTime()).isNull();
    InstanceData duplicateInstanceData = instanceDataDao.upsert(instanceInfo());
    assertThat(duplicateInstanceData).isNotNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldUpsertInstanceStartStopEvent() {
    instanceDataDao.upsert(instanceInfo());
    instanceDataDao.upsert(instanceEvent(START_INSTANT, EventType.START));
    InstanceData updatedStartInstanceData = instanceDataDao.fetchInstanceData(ACCOUNT_ID, RUNNING_INSTANCE_ID);
    assertThat(updatedStartInstanceData.getUsageStartTime()).isEqualTo(START_INSTANT);
    assertThat(updatedStartInstanceData.getUsageStopTime()).isNull();
    instanceDataDao.upsert(instanceEvent(END_INSTANT, EventType.STOP));
    InstanceData updatedStopInstanceData = instanceDataDao.fetchInstanceData(ACCOUNT_ID, RUNNING_INSTANCE_ID);
    assertThat(updatedStopInstanceData.getUsageStopTime()).isEqualTo(END_INSTANT);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldReturnNullWhenUpsertInstanceEvent() {
    InstanceData instanceData = instanceDataDao.upsert(instanceEvent(START_INSTANT, EventType.START));
    assertThat(instanceData).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldUpdateInstanceState() {
    instanceDataDao.create(instanceData(RUNNING_INSTANCE_ID, InstanceState.RUNNING));
    InstanceData instanceData = instanceDataDao.fetchInstanceData(ACCOUNT_ID, RUNNING_INSTANCE_ID);
    boolean instanceUpdated = instanceDataDao.updateInstanceState(
        instanceData, END_INSTANT, InstanceDataKeys.usageStopTime, InstanceState.STOPPED);
    assertThat(instanceUpdated).isTrue();
    InstanceData updatedInstanceData = instanceDataDao.fetchInstanceData(ACCOUNT_ID, RUNNING_INSTANCE_ID);
    assertThat(updatedInstanceData.getInstanceState()).isEqualTo(InstanceState.STOPPED);
    assertThat(updatedInstanceData.getUsageStopTime()).isEqualTo(END_INSTANT);
  }

  private List<InstanceState> getActiveInstanceState() {
    return new ArrayList<>(Arrays.asList(InstanceState.INITIALIZING, InstanceState.RUNNING));
  }

  private InstanceData instanceData(String instanceId, InstanceState instanceState) {
    return InstanceData.builder()
        .instanceId(instanceId)
        .instanceName(INSTANCE_NAME)
        .accountId(ACCOUNT_ID)
        .settingId(CLOUD_PROVIDER_ID)
        .instanceState(instanceState)
        .clusterName(CLUSTER_NAME)
        .clusterId(CLUSTER_ID)
        .instanceType(InstanceType.EC2_INSTANCE)
        .instanceState(InstanceState.RUNNING)
        .usageStartTime(START_INSTANT)
        .build();
  }

  private InstanceInfo instanceInfo() {
    return InstanceInfo.builder()
        .accountId(ACCOUNT_ID)
        .instanceName(INSTANCE_NAME)
        .instanceId(RUNNING_INSTANCE_ID)
        .instanceType(InstanceType.K8S_POD)
        .settingId(CLOUD_PROVIDER_ID)
        .clusterId(CLUSTER_ID)
        .clusterName(CLUSTER_NAME)
        .instanceState(InstanceState.INITIALIZING)
        .harnessServiceInfo(harnessServiceInfo())
        .resource(resource())
        .allocatableResource(resource())
        .metaData(metaData())
        .labels(label())
        .build();
  }

  private InstanceEvent instanceEvent(Instant instant, EventType eventType) {
    return InstanceEvent.builder()
        .instanceId(RUNNING_INSTANCE_ID)
        .timestamp(instant)
        .accountId(ACCOUNT_ID)
        .clusterId(CLUSTER_ID)
        .cloudProviderId(CLOUD_PROVIDER_ID)
        .type(eventType)
        .instanceName(INSTANCE_NAME)
        .build();
  }

  private HarnessServiceInfo harnessServiceInfo() {
    return new HarnessServiceInfo(
        "serviceId", "appId", "cloudProviderId", "envId", "infraMappingId", "deploymentSummaryId");
  }

  private Resource resource() {
    return Resource.builder().cpuUnits(DEFAULT_INSTANCE_CPU).memoryMb(DEFAULT_INSTANCE_MEMORY).build();
  }

  private Map<String, String> metaData() {
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, InstanceMetaDataConstants.INSTANCE_FAMILY);
    metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_ID, InstanceMetaDataConstants.PARENT_RESOURCE_ID);
    metaData.put(InstanceMetaDataConstants.NAMESPACE, InstanceMetaDataConstants.NAMESPACE);
    metaData.put(InstanceMetaDataConstants.WORKLOAD_NAME, InstanceMetaDataConstants.WORKLOAD_NAME);
    metaData.put(InstanceMetaDataConstants.WORKLOAD_TYPE, InstanceMetaDataConstants.WORKLOAD_TYPE);
    return metaData;
  }

  private Map<String, String> label() {
    Map<String, String> label = new HashMap<>();
    label.put("component", "kube-proxy");
    label.put("tier", "node");
    return label;
  }
}