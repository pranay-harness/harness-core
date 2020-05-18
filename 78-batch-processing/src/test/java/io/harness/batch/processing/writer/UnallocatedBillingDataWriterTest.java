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

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UnallocatedBillingDataServiceImpl;
import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.ccm.CCMJobConstants;
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
import org.springframework.batch.core.JobParameters;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class UnallocatedBillingDataWriterTest extends CategoryTest {
  @Inject @InjectMocks private UnallocatedBillingDataWriter unallocatedBillingDataWriter;
  @Mock private BillingDataServiceImpl billingDataService;
  @Mock private UnallocatedBillingDataServiceImpl unallocatedBillingDataService;
  @Mock private JobParameters parameters;

  private final Instant NOW = Instant.now();
  private final long START_TIME_MILLIS = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME_MILLIS = NOW.toEpochMilli();
  private final String CLUSTER_ID_1 = "clusterId_1";
  private final String CLUSTER_ID_2 = "clusterId_2";
  private final double COST_POD = 2.0;
  private final double SYSTEM_COST_POD = 0.0;
  private final double COST_NODE = 6.0;
  private final double SYSTEM_COST_NODE = 0.75;
  // ECS Data
  private final String CLUSTER_ID_3 = "clusterId_3";
  private final String CLUSTER_ID_4 = "clusterId_4";
  private final double COST_CONTAINER = 2.5;
  private final double SYSTEM_COST_CONTAINER = 0.5;
  private final double COST_TASK = 1.2;
  private final double SYSTEM_COST_TASK = 0.0;

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
    when(parameters.getString(CCMJobConstants.BATCH_JOB_TYPE))
        .thenReturn(BatchJobType.UNALLOCATED_BILLING_HOURLY.name());

    when(billingDataService.create(any(), any())).thenReturn(true);
    // K8s Mock Data
    UnallocatedCostData unallocatedCostDataPodCluster1 =
        getMockUnallocatedCostData(CLUSTER_ID_1, InstanceType.K8S_POD.name(), COST_POD, SYSTEM_COST_POD, COST_POD * .5,
            SYSTEM_COST_POD, COST_POD * .5, SYSTEM_COST_POD);
    UnallocatedCostData unallocatedCostDataNodeCluster1 =
        getMockUnallocatedCostData(CLUSTER_ID_1, InstanceType.K8S_NODE.name(), COST_NODE, SYSTEM_COST_NODE,
            COST_NODE * .3, SYSTEM_COST_NODE * .3, COST_NODE * .7, SYSTEM_COST_NODE * .7);
    UnallocatedCostData unallocatedCostDataPodCluster2 =
        getMockUnallocatedCostData(CLUSTER_ID_2, InstanceType.K8S_POD.name(), COST_POD, SYSTEM_COST_POD, COST_POD * .5,
            SYSTEM_COST_POD, COST_POD * .5, SYSTEM_COST_POD);
    UnallocatedCostData unallocatedCostDataNodeCluster2 =
        getMockUnallocatedCostData(CLUSTER_ID_2, InstanceType.K8S_NODE.name(), COST_NODE, SYSTEM_COST_NODE,
            COST_NODE * .7, SYSTEM_COST_NODE * .7, COST_NODE * .3, SYSTEM_COST_NODE * .3);

    // ECS Mock Data
    UnallocatedCostData unallocatedCostDataContainerCluster3 = getMockUnallocatedCostData(CLUSTER_ID_3,
        InstanceType.ECS_CONTAINER_INSTANCE.name(), COST_CONTAINER, SYSTEM_COST_CONTAINER, COST_CONTAINER * .6,
        SYSTEM_COST_CONTAINER * .6, COST_CONTAINER * .4, SYSTEM_COST_CONTAINER * .4);
    UnallocatedCostData unallocatedCostDataTaskCluster3 =
        getMockUnallocatedCostData(CLUSTER_ID_3, InstanceType.ECS_TASK_EC2.name(), COST_TASK, SYSTEM_COST_TASK,
            COST_TASK * .5, SYSTEM_COST_TASK, COST_TASK * .5, SYSTEM_COST_TASK);
    UnallocatedCostData unallocatedCostDataContainerCluster4 = getMockUnallocatedCostData(CLUSTER_ID_4,
        InstanceType.ECS_CONTAINER_INSTANCE.name(), COST_CONTAINER, SYSTEM_COST_CONTAINER, COST_CONTAINER * .4,
        SYSTEM_COST_CONTAINER * .4, COST_CONTAINER * .6, SYSTEM_COST_CONTAINER * .6);
    UnallocatedCostData unallocatedCostDataTaskCluster4 =
        getMockUnallocatedCostData(CLUSTER_ID_4, InstanceType.ECS_TASK_EC2.name(), COST_TASK, SYSTEM_COST_TASK,
            COST_TASK * .75, SYSTEM_COST_TASK, COST_TASK * .25, SYSTEM_COST_TASK);
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

    when(unallocatedBillingDataService.getCommonFields(eq(ACCOUNT_ID), eq(CLUSTER_ID_1), anyLong(), anyLong(), any()))
        .thenReturn(clusterCostDataBuilder.accountId(ACCOUNT_ID).clusterType(K8S_CLUSTER_TYPE).build());
    when(unallocatedBillingDataService.getCommonFields(eq(ACCOUNT_ID), eq(CLUSTER_ID_2), anyLong(), anyLong(), any()))
        .thenReturn(clusterCostDataBuilder.accountId(ACCOUNT_ID).clusterType(K8S_CLUSTER_TYPE).build());
    when(unallocatedBillingDataService.getCommonFields(eq(ACCOUNT_ID), eq(CLUSTER_ID_3), anyLong(), anyLong(), any()))
        .thenReturn(clusterCostDataBuilder.accountId(ACCOUNT_ID).clusterType(ECS_CLUSTER_TYPE).build());
    when(unallocatedBillingDataService.getCommonFields(eq(ACCOUNT_ID), eq(CLUSTER_ID_4), anyLong(), anyLong(), any()))
        .thenReturn(clusterCostDataBuilder.accountId(ACCOUNT_ID).clusterType(ECS_CLUSTER_TYPE).build());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteUnallocatedData() {
    // Calling Writer
    unallocatedBillingDataWriter.write(Collections.singletonList(unallocatedCostDataList));
    ArgumentCaptor<InstanceBillingData> instanceBillingDataArgumentCaptor =
        ArgumentCaptor.forClass(InstanceBillingData.class);
    verify(billingDataService, atMost(4)).create(instanceBillingDataArgumentCaptor.capture(), any());
    List<InstanceBillingData> instanceUtilizationData = instanceBillingDataArgumentCaptor.getAllValues();
    assertThat(instanceUtilizationData.get(0).getClusterId()).isEqualTo(CLUSTER_ID_1);
    assertThat(instanceUtilizationData.get(0).getClusterType()).isEqualTo(K8S_CLUSTER_TYPE);
    assertThat(instanceUtilizationData.get(0).getBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_NODE - COST_POD - SYSTEM_COST_NODE));
    assertThat(instanceUtilizationData.get(0).getCpuBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_NODE * .3 - COST_POD * .5 - SYSTEM_COST_NODE * .3));
    assertThat(instanceUtilizationData.get(0).getMemoryBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_NODE * .7 - COST_POD * .5 - SYSTEM_COST_NODE * .7));
    assertThat(instanceUtilizationData.get(1).getClusterId()).isEqualTo(CLUSTER_ID_2);
    assertThat(instanceUtilizationData.get(1).getClusterType()).isEqualTo(K8S_CLUSTER_TYPE);
    assertThat(instanceUtilizationData.get(1).getBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_NODE - COST_POD - SYSTEM_COST_NODE));
    assertThat(instanceUtilizationData.get(1).getCpuBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_NODE * .7 - COST_POD * .5 - SYSTEM_COST_NODE * .7));
    assertThat(instanceUtilizationData.get(1).getMemoryBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_NODE * .3 - COST_POD * .5 - SYSTEM_COST_NODE * .3));
    assertThat(instanceUtilizationData.get(2).getClusterId()).isEqualTo(CLUSTER_ID_3);
    assertThat(instanceUtilizationData.get(2).getClusterType()).isEqualTo(ECS_CLUSTER_TYPE);
    assertThat(instanceUtilizationData.get(2).getBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_CONTAINER - COST_TASK - SYSTEM_COST_CONTAINER));
    assertThat(instanceUtilizationData.get(2).getCpuBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_CONTAINER * .6 - COST_TASK * .5 - SYSTEM_COST_CONTAINER * .6));
    assertThat(instanceUtilizationData.get(2).getMemoryBillingAmount())
        .isEqualTo(BigDecimal.valueOf(COST_CONTAINER * .4 - COST_TASK * .5 - SYSTEM_COST_CONTAINER * .4));
    assertThat(instanceUtilizationData.get(3).getClusterId()).isEqualTo(CLUSTER_ID_4);
    assertThat(instanceUtilizationData.get(3).getBillingAmount()).isEqualTo(BigDecimal.ZERO);
    assertThat(instanceUtilizationData.get(3).getCpuBillingAmount()).isEqualTo(BigDecimal.ZERO);
    assertThat(instanceUtilizationData.get(3).getMemoryBillingAmount()).isEqualTo(BigDecimal.ZERO);
    assertThat(instanceUtilizationData.get(3).getClusterType()).isEqualTo(ECS_CLUSTER_TYPE);
  }

  UnallocatedCostData getMockUnallocatedCostData(String clusterId, String instanceType, double cost, double systemCost,
      double cpuCost, double cpuSystemCost, double memoryCost, double memorySystemCost) {
    return UnallocatedCostData.builder()
        .clusterId(clusterId)
        .accountId(ACCOUNT_ID)
        .instanceType(instanceType)
        .cost(cost)
        .systemCost(systemCost)
        .cpuCost(cpuCost)
        .cpuSystemCost(cpuSystemCost)
        .memoryCost(memoryCost)
        .memorySystemCost(memorySystemCost)
        .startTime(START_TIME_MILLIS)
        .endTime(END_TIME_MILLIS)
        .build();
  }
}
