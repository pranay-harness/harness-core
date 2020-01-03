package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.ClusterCostData.ClusterCostDataBuilder;
import static io.harness.batch.processing.ccm.ClusterCostData.builder;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UnallocatedBillingDataServiceImpl;
import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.UnallocatedCostData;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class UnallocatedBillingDataWriterTest extends CategoryTest {
  @InjectMocks private UnallocatedBillingDataWriter unallocatedBillingDataWriter;
  @Mock private BillingDataServiceImpl billingDataService;
  @Mock private UnallocatedBillingDataServiceImpl unallocatedBillingDataService;

  private final Instant NOW = Instant.now();
  private final long START_TIME_MILLIS = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME_MILLIS = NOW.toEpochMilli();
  private final String CLUSTER_ID_1 = "clusterId_1";
  private final String CLUSTER_ID_2 = "clusterId_2";
  private final double COST_POD = 2.0;
  private final double COST_NODE = 4.0;
  // ECS Data
  private final String CLUSTER_ID_3 = "clusterId_3";
  private final String CLUSTER_ID_4 = "clusterId_4";
  private final double COST_CONTAINER = 4.0;
  private final double COST_TASK = 2.0;

  // Common Data
  private static final String BILLING_ACCOUNT_ID = "billingAccountId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String CLUSTER_NAME = "clusterName";
  private static final String SETTING_ID = "settingId";
  private static final String REGION = "region";
  private static final String CLOUD_PROVIDER = "cloudProvider";
  private static final String K8S_CLUSTER_TYPE = ClusterType.K8S.name();
  private static final String ECS_CLUSTER_TYPE = ClusterType.ECS.name();
  private static final String WORKLOAD_TYPE = "workloadType";

  List<UnallocatedCostData> unallocatedCostDataList;

  @Before
  public void setup() {
    when(billingDataService.create(any())).thenReturn(true);
    // K8s Mock Data
    UnallocatedCostData unallocatedCostDataPodCluster1 =
        getMockUnallocatedCostData(CLUSTER_ID_1, InstanceType.K8S_POD.name(), COST_POD);
    UnallocatedCostData unallocatedCostDataNodeCluster1 =
        getMockUnallocatedCostData(CLUSTER_ID_1, InstanceType.K8S_NODE.name(), COST_NODE);
    UnallocatedCostData unallocatedCostDataPodCluster2 =
        getMockUnallocatedCostData(CLUSTER_ID_2, InstanceType.K8S_POD.name(), COST_POD);
    UnallocatedCostData unallocatedCostDataNodeCluster2 =
        getMockUnallocatedCostData(CLUSTER_ID_2, InstanceType.K8S_NODE.name(), COST_NODE);

    // ECS Mock Data
    UnallocatedCostData unallocatedCostDataContainerCluster3 =
        getMockUnallocatedCostData(CLUSTER_ID_3, InstanceType.ECS_CONTAINER_INSTANCE.name(), COST_CONTAINER);
    UnallocatedCostData unallocatedCostDataTaskCluster3 =
        getMockUnallocatedCostData(CLUSTER_ID_3, InstanceType.ECS_TASK_EC2.name(), COST_TASK);
    UnallocatedCostData unallocatedCostDataContainerCluster4 =
        getMockUnallocatedCostData(CLUSTER_ID_4, InstanceType.ECS_CONTAINER_INSTANCE.name(), COST_CONTAINER);
    UnallocatedCostData unallocatedCostDataTaskCluster4 =
        getMockUnallocatedCostData(CLUSTER_ID_4, InstanceType.ECS_TASK_EC2.name(), COST_TASK);
    // Creating a List
    unallocatedCostDataList = Arrays.asList(unallocatedCostDataPodCluster1, unallocatedCostDataNodeCluster1,
        unallocatedCostDataNodeCluster2, unallocatedCostDataPodCluster2, unallocatedCostDataContainerCluster3,
        unallocatedCostDataTaskCluster3, unallocatedCostDataTaskCluster4, unallocatedCostDataContainerCluster4);

    ClusterCostDataBuilder clusterCostDataBuilder = builder()
                                                        .billingAccountId(BILLING_ACCOUNT_ID)
                                                        .accountId(ACCOUNT_ID)
                                                        .clusterName(CLUSTER_NAME)
                                                        .settingId(SETTING_ID)
                                                        .region(REGION)
                                                        .cloudProvider(CLOUD_PROVIDER)
                                                        .workloadType(WORKLOAD_TYPE);

    when(unallocatedBillingDataService.getCommonFields(eq(CLUSTER_ID_1), anyLong(), anyLong()))
        .thenReturn(clusterCostDataBuilder.clusterType(K8S_CLUSTER_TYPE).build());
    when(unallocatedBillingDataService.getCommonFields(eq(CLUSTER_ID_2), anyLong(), anyLong()))
        .thenReturn(clusterCostDataBuilder.clusterType(K8S_CLUSTER_TYPE).build());
    when(unallocatedBillingDataService.getCommonFields(eq(CLUSTER_ID_3), anyLong(), anyLong()))
        .thenReturn(clusterCostDataBuilder.clusterType(ECS_CLUSTER_TYPE).build());
    when(unallocatedBillingDataService.getCommonFields(eq(CLUSTER_ID_4), anyLong(), anyLong()))
        .thenReturn(clusterCostDataBuilder.clusterType(ECS_CLUSTER_TYPE).build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteUnallocatedData() {
    // Calling Writer
    unallocatedBillingDataWriter.write(Collections.singletonList(unallocatedCostDataList));
    ArgumentCaptor<InstanceBillingData> instanceBillingDataArgumentCaptor =
        ArgumentCaptor.forClass(InstanceBillingData.class);
    verify(billingDataService, atMost(4)).create(instanceBillingDataArgumentCaptor.capture());
    List<InstanceBillingData> instanceUtilizationData = instanceBillingDataArgumentCaptor.getAllValues();
    assertThat(instanceUtilizationData.get(0).getClusterId()).isEqualTo(CLUSTER_ID_1);
    assertThat(instanceUtilizationData.get(0).getClusterType()).isEqualTo(K8S_CLUSTER_TYPE);
    assertThat(instanceUtilizationData.get(0).getBillingAmount()).isEqualTo(BigDecimal.valueOf(COST_NODE - COST_POD));
    assertThat(instanceUtilizationData.get(1).getClusterId()).isEqualTo(CLUSTER_ID_2);
    assertThat(instanceUtilizationData.get(1).getClusterType()).isEqualTo(K8S_CLUSTER_TYPE);
    assertThat(instanceUtilizationData.get(1).getBillingAmount()).isEqualTo(BigDecimal.valueOf(COST_NODE - COST_POD));
    assertThat(instanceUtilizationData.get(2).getClusterId()).isEqualTo(CLUSTER_ID_3);
    assertThat(instanceUtilizationData.get(2).getClusterType()).isEqualTo(ECS_CLUSTER_TYPE);
    assertThat(instanceUtilizationData.get(2).getBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_CONTAINER - COST_TASK));
    assertThat(instanceUtilizationData.get(3).getClusterId()).isEqualTo(CLUSTER_ID_4);
    assertThat(instanceUtilizationData.get(3).getBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_CONTAINER - COST_TASK));
    assertThat(instanceUtilizationData.get(3).getClusterType()).isEqualTo(ECS_CLUSTER_TYPE);
  }

  UnallocatedCostData getMockUnallocatedCostData(String clusterId, String instanceType, double cost) {
    return UnallocatedCostData.builder()
        .clusterId(clusterId)
        .instanceType(instanceType)
        .cost(cost)
        .startTime(START_TIME_MILLIS)
        .endTime(END_TIME_MILLIS)
        .build();
  }
}
