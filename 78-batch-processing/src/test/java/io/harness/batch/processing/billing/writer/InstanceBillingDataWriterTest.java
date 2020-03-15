package io.harness.batch.processing.billing.writer;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.ecs.model.LaunchType;
import io.harness.CategoryTest;
import io.harness.batch.processing.billing.service.BillingAmountBreakup;
import io.harness.batch.processing.billing.service.BillingCalculationService;
import io.harness.batch.processing.billing.service.BillingData;
import io.harness.batch.processing.billing.service.IdleCostData;
import io.harness.batch.processing.billing.service.SystemCostData;
import io.harness.batch.processing.billing.service.UtilizationData;
import io.harness.batch.processing.billing.timeseries.data.InstanceBillingData;
import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.billing.writer.support.BillingDataGenerationValidator;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;
import org.springframework.test.context.ActiveProfiles;
import software.wings.beans.instance.HarnessServiceInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@ActiveProfiles("test")
@RunWith(MockitoJUnitRunner.class)
public class InstanceBillingDataWriterTest extends CategoryTest {
  private static final String PARENT_RESOURCE_ID = "parent_resource_id";
  private static final String ACCOUNT_ID = "account_id";
  private static final String INSTANCE_ID = "instance_id";
  private static final String SERVICE_ID = "service_id";
  private static final String APP_ID = "app_id";
  private static final String CLOUD_PROVIDER_ID = "cloud_provider_id";
  private static final String ENV_ID = "env_id";
  private static final String INFRA_MAPPING_ID = "infra_mapping_id";
  private static final String DEPLOYMENT_SUMMARY_ID = "deployment_summary_id";
  private static final String CLUSTER_ID = "cluster_id";
  private static final String CLUSTER_NAME = "cluster_name";

  private final double CPU_UNIT_SECONDS = 400;
  private final double MEMORY_MB_SECONDS = 400;
  private final double USAGE_DURATION_SECONDS = 400;
  private final double CPU_UTILIZATION = 1;
  private final double MEMORY_UTILIZATION = 1;

  private final Instant NOW = Instant.now();
  private final long START_TIME_MILLIS = NOW.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private final long END_TIME_MILLIS = NOW.toEpochMilli();

  @InjectMocks private InstanceBillingDataWriter instanceBillingDataWriter;
  @Mock private BillingDataServiceImpl billingDataService;
  @Mock private JobParameters parameters;
  @Mock private BillingCalculationService billingCalculationService;
  @Mock private UtilizationDataServiceImpl utilizationDataService;
  @Mock private BillingDataGenerationValidator billingDataGenerationValidator;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetValueForKeyFromInstanceMetaData() {
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_ID, PARENT_RESOURCE_ID);
    InstanceData instanceData = InstanceData.builder().metaData(metaData).build();
    String parentInstanceId = instanceBillingDataWriter.getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.PARENT_RESOURCE_ID, instanceData);
    assertThat(parentInstanceId).isEqualTo(PARENT_RESOURCE_ID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetNullValueForKeyFromInstanceMetaData() {
    InstanceData instanceData = InstanceData.builder().build();
    String parentInstanceId = instanceBillingDataWriter.getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.PARENT_RESOURCE_ID, instanceData);
    assertThat(parentInstanceId).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetEmptyHarnessServiceInfo() {
    InstanceData instanceData = InstanceData.builder().build();
    HarnessServiceInfo harnessServiceInfo = instanceBillingDataWriter.getHarnessServiceInfo(instanceData);
    assertThat(harnessServiceInfo).isNotNull();
    assertThat(harnessServiceInfo.getAppId()).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetHarnessServiceInfo() {
    InstanceData instanceData = InstanceData.builder().harnessServiceInfo(getHarnessServiceInfo()).build();
    HarnessServiceInfo harnessServiceInfo = instanceBillingDataWriter.getHarnessServiceInfo(instanceData);
    assertThat(harnessServiceInfo).isNotNull();
    assertThat(harnessServiceInfo.getAppId()).isEqualTo(APP_ID);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetCloudServiceNameWhenNotPresentForEC2() {
    Map<String, String> metaDataMap = new HashMap<>();
    metaDataMap.put(InstanceMetaDataConstants.LAUNCH_TYPE, LaunchType.EC2.name());
    InstanceData instanceData =
        InstanceData.builder().instanceType(InstanceType.ECS_TASK_EC2).metaData(metaDataMap).build();
    String cloudServiceName = instanceBillingDataWriter.getCloudServiceName(instanceData);
    assertThat(cloudServiceName).isEqualTo("dangling_task_service_ec2");
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetCloudServiceNameWhenNotPresentForFargate() {
    Map<String, String> metaDataMap = new HashMap<>();
    metaDataMap.put(InstanceMetaDataConstants.LAUNCH_TYPE, LaunchType.FARGATE.name());
    InstanceData instanceData =
        InstanceData.builder().instanceType(InstanceType.ECS_TASK_FARGATE).metaData(metaDataMap).build();
    String cloudServiceName = instanceBillingDataWriter.getCloudServiceName(instanceData);
    assertThat(cloudServiceName).isEqualTo("dangling_task_service_fargate");
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGetCloudServiceNameWhenNotPresent() {
    Map<String, String> metaDataMap = new HashMap<>();
    InstanceData instanceData = InstanceData.builder().build();
    String cloudServiceName = instanceBillingDataWriter.getCloudServiceName(instanceData);
    assertThat(cloudServiceName).isNull();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testWriteBillingData() throws Exception {
    Map<String, String> metaDataMap = new HashMap<>();
    metaDataMap.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
    Map<String, UtilizationData> utilizationDataForInstances = new HashMap<>();
    utilizationDataForInstances.put(INSTANCE_ID,
        UtilizationData.builder()
            .maxCpuUtilization(CPU_UTILIZATION)
            .maxMemoryUtilization(MEMORY_UTILIZATION)
            .avgCpuUtilization(CPU_UTILIZATION)
            .avgMemoryUtilization(MEMORY_UTILIZATION)
            .build());
    InstanceData instanceData = InstanceData.builder()
                                    .instanceType(InstanceType.EC2_INSTANCE)
                                    .metaData(metaDataMap)
                                    .accountId(ACCOUNT_ID)
                                    .instanceId(INSTANCE_ID)
                                    .clusterId(CLUSTER_ID)
                                    .clusterName(CLUSTER_NAME)
                                    .harnessServiceInfo(getHarnessServiceInfo())
                                    .build();
    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(START_TIME_MILLIS));
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(END_TIME_MILLIS));
    when(utilizationDataService.getUtilizationDataForInstances(any(), any(), any(), any(), any(), any()))
        .thenReturn(utilizationDataForInstances);
    when(billingCalculationService.getInstanceBillingAmount(any(), any(), any(), any()))
        .thenReturn(new BillingData(BillingAmountBreakup.builder().billingAmount(BigDecimal.ONE).build(),
            new IdleCostData(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
            new SystemCostData(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO), USAGE_DURATION_SECONDS,
            CPU_UNIT_SECONDS, MEMORY_MB_SECONDS));
    when(billingDataGenerationValidator.shouldGenerateBillingData(
             ACCOUNT_ID, CLUSTER_ID, Instant.ofEpochMilli(START_TIME_MILLIS)))
        .thenReturn(true);
    instanceBillingDataWriter.write(Arrays.asList(instanceData));
    ArgumentCaptor<InstanceBillingData> instanceBillingDataArgumentCaptor =
        ArgumentCaptor.forClass(InstanceBillingData.class);
    verify(billingDataService).create(instanceBillingDataArgumentCaptor.capture());
    InstanceBillingData instanceBillingData = instanceBillingDataArgumentCaptor.getValue();
    assertThat(instanceBillingData.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(instanceBillingData.getClusterId()).isEqualTo(null);
    assertThat(instanceBillingData.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(instanceBillingData.getBillingAmount()).isEqualTo(BigDecimal.ONE);
    assertThat(instanceBillingData.getIdleCost()).isEqualTo(BigDecimal.ZERO);
    assertThat(instanceBillingData.getUsageDurationSeconds()).isEqualTo(USAGE_DURATION_SECONDS);
    assertThat(instanceBillingData.getCpuUnitSeconds()).isEqualTo(CPU_UNIT_SECONDS);
    assertThat(instanceBillingData.getMemoryMbSeconds()).isEqualTo(MEMORY_MB_SECONDS);
    assertThat(instanceBillingData.getCloudProvider()).isEqualTo(CloudProvider.AWS.name());
    assertThat(instanceBillingData.getStartTimestamp()).isEqualTo(START_TIME_MILLIS);
    assertThat(instanceBillingData.getEndTimestamp()).isEqualTo(END_TIME_MILLIS);
    assertThat(instanceBillingData.getAppId()).isEqualTo(APP_ID);
    assertThat(instanceBillingData.getServiceId()).isEqualTo(SERVICE_ID);
    assertThat(instanceBillingData.getMaxCpuUtilization()).isEqualTo(CPU_UTILIZATION);
    assertThat(instanceBillingData.getMaxMemoryUtilization()).isEqualTo(MEMORY_UTILIZATION);
    assertThat(instanceBillingData.getAvgCpuUtilization()).isEqualTo(CPU_UTILIZATION);
    assertThat(instanceBillingData.getAvgMemoryUtilization()).isEqualTo(MEMORY_UTILIZATION);
  }

  private HarnessServiceInfo getHarnessServiceInfo() {
    return new HarnessServiceInfo(
        SERVICE_ID, APP_ID, CLOUD_PROVIDER_ID, ENV_ID, INFRA_MAPPING_ID, DEPLOYMENT_SUMMARY_ID);
  }
}
