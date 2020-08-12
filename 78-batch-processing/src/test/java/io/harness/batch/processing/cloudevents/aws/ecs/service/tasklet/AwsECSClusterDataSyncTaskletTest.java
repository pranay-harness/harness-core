package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet;

import static io.harness.rule.OwnerRule.HITESH;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;

import com.amazonaws.services.ecs.model.Service;
import io.harness.CategoryTest;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.cloudevents.aws.ecs.service.CEClusterDao;
import io.harness.batch.processing.cloudevents.aws.ecs.service.intfc.AwsECSClusterService;
import io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc.AwsEC2HelperService;
import io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc.AwsECSHelperService;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.EcsMetricClient;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response.EcsUtilizationData;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response.MetricValue;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.service.intfc.InstanceResourceService;
import io.harness.category.element.UnitTests;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;
import io.harness.ccm.setup.CECloudAccountDao;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;
import software.wings.beans.ce.CECluster;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingValue;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AwsECSClusterDataSyncTaskletTest extends CategoryTest {
  @InjectMocks private AwsECSClusterDataSyncTasklet awsECSClusterDataSyncTasklet;
  @Mock private CEClusterDao ceClusterDao;
  @Mock private EcsMetricClient ecsMetricClient;
  @Mock private InstanceDataDao instanceDataDao;
  @Mock private CECloudAccountDao ceCloudAccountDao;
  @Mock private AwsECSHelperService awsECSHelperService;
  @Mock private AwsEC2HelperService awsEC2HelperService;
  @Mock protected InstanceDataService instanceDataService;
  @Mock private UtilizationDataServiceImpl utilizationDataService;
  @Mock protected InstanceResourceService instanceResourceService;
  @Mock private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Mock private AwsECSClusterService awsECSClusterService;
  @Mock private LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;

  @InjectMocks AwsECSClusterSyncTasklet awsECSClusterSyncTasklet;

  private final String REGION = "us-east-1";
  private final String ACCOUNT_ID = "accountId";
  private final String INFRA_ACCOUNT_ID = "infraAccountId";
  private final String CLUSTER_ID = "clusterId";
  private final String SETTING_ID = "settingId";
  private final String CLUSTER_NAME = "ecs-ccm-cluster";
  private final String PERPETUAL_TASK_ID = "perpetualTaskId";
  private final String CLUSTER_ARN = "arn:aws:ecs:us-east-2:132359207506:cluster/ecs-ccm-cluster";
  private final String AWS_ACCOUNT_ID = "awsAccountId";
  private final String AWS_ACCOUNT_NAME = "awsAccountName";
  private final String AWS_MASTER_ACCOUNT_ID = "awsMasterAccountId";
  private final String EXTERNAL_ID = "EXTERNAL_ID" + this.getClass().getSimpleName();
  private final String ROLE_ARN = "ROLE_ARN" + this.getClass().getSimpleName();

  private static final String accountId = "accountId";
  private static final String accountName = "accountName";
  private static final String settingId = "settingId";
  private static final String masterAccountId = "masterAccountId";
  private static final String curReportName = "curReportName";
  private static final String dataSetId = "datasetId";
  private static final String transferJobName = "transferJobName";
  private static final Instant instant = Instant.now().truncatedTo(ChronoUnit.HOURS);
  private static final long startTime = instant.minus(1, ChronoUnit.HOURS).toEpochMilli();
  private static final long endTime = instant.toEpochMilli();
  private static final String scheduledQueryName = "scheduledQueryName";
  private static final String preAggQueryName = "preAggQueryName";

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
  }

  private MetricValue getMetricValue(String metricName, String statistic, long startTime, double value) {
    return MetricValue.builder()
        .metricName(metricName)
        .statistic(statistic)
        .timestamps(singletonList(Date.from(Instant.ofEpochMilli(startTime))))
        .values(singletonList(value))
        .build();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldQueryEcsMetricClientAndPublishUtilizationMessages() throws Exception {
    Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);
    final ImmutableList<Service> services =
        ImmutableList.of(new Service()
                             .withServiceArn("arn:aws:ecs:us-east-2:132359207506:service/ccm-test-service")
                             .withServiceName("ccm-test-service"),
            new Service()
                .withServiceArn("arn:aws:ecs:us-east-1:132359207506:service/ccm-test-service")
                .withServiceName("ccm-test-service"));
    List<EcsUtilizationData> utilizationMessages =
        ImmutableList.of(EcsUtilizationData.builder()
                             .clusterArn("cluster1-arn")
                             .clusterName("clusterName")
                             .clusterId("BNirC4Mg48Vf2n7LAznpPgqGggk=")
                             .settingId(settingId)
                             .metricValues(Arrays.asList(getMetricValue("CPUUtilization", "Average", startTime, 78.3),
                                 getMetricValue("CPUUtilization", "Maximum", startTime, 78.3),
                                 getMetricValue("MemoryUtilization", "Average", startTime, 78.3),
                                 getMetricValue("MemoryUtilization", "Maximum", startTime, 78.3)))
                             .build(),

            EcsUtilizationData.builder()
                .clusterArn("cluster1-arn")
                .clusterName("clusterName")
                .serviceArn("svc2-arn")
                .serviceName("svc2")
                .clusterId("BNirC4Mg48Vf2n7LAznpPgqGggk=")
                .settingId(settingId)
                .metricValues(Arrays.asList(getMetricValue("CPUUtilization", "Average", endTime, 66.6),
                    getMetricValue("CPUUtilization", "Maximum", endTime, 66.6),
                    getMetricValue("MemoryUtilization", "Average", endTime, 66.6),
                    getMetricValue("MemoryUtilization", "Maximum", endTime, 66.6)))
                .build());

    AwsCrossAccountAttributes awsCrossAccountAttributes = AwsCrossAccountAttributes.builder().build();
    CECluster ceCluster = CECluster.builder()
                              .accountId(ACCOUNT_ID)
                              .region(REGION)
                              .infraAccountId(INFRA_ACCOUNT_ID)
                              .clusterName(CLUSTER_NAME)
                              .clusterName(CLUSTER_ARN)
                              .build();
    given(awsECSHelperService.listServicesForCluster(awsCrossAccountAttributes, REGION, CLUSTER_ARN))
        .willReturn(services);
    given(ecsMetricClient.getUtilizationMetrics(any(), any(), any(), any(), any(), any()))
        .willReturn(utilizationMessages);

    awsECSClusterDataSyncTasklet.publishUtilizationMetrics(awsCrossAccountAttributes, ceCluster);

    ArgumentCaptor<List<InstanceUtilizationData>> utilizationDataArgumentCaptor =
        ArgumentCaptor.forClass((Class) List.class);
    verify(utilizationDataService).create(utilizationDataArgumentCaptor.capture());
    List<List<InstanceUtilizationData>> allValues = utilizationDataArgumentCaptor.getAllValues();
    assertThat(allValues.get(0)).hasSize(2);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldSyncCluster() throws Exception {
    ChunkContext chunkContext = mock(ChunkContext.class);
    StepContext stepContext = mock(StepContext.class);
    StepExecution stepExecution = mock(StepExecution.class);
    JobParameters parameters = mock(JobParameters.class);
    when(chunkContext.getStepContext()).thenReturn(stepContext);
    when(stepContext.getStepExecution()).thenReturn(stepExecution);
    when(stepExecution.getJobParameters()).thenReturn(parameters);
    AwsCrossAccountAttributes awsCrossAccountAttributes = getAwsCrossAccountAttributes();

    when(parameters.getString(CCMJobConstants.JOB_START_DATE)).thenReturn(String.valueOf(startTime));
    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(ACCOUNT_ID);
    when(parameters.getString(CCMJobConstants.JOB_END_DATE)).thenReturn(String.valueOf(endTime));
    when(ceClusterDao.getCECluster(ACCOUNT_ID)).thenReturn(singletonList(getCeCluster()));
    when(ceCloudAccountDao.getByAWSAccountId(ACCOUNT_ID))
        .thenReturn(Collections.singletonList(CECloudAccount.builder()
                                                  .infraAccountId(AWS_ACCOUNT_ID)
                                                  .awsCrossAccountAttributes(awsCrossAccountAttributes)
                                                  .accountName(AWS_ACCOUNT_NAME)
                                                  .build()));

    SettingValue settingValue = CEAwsConfig.builder().awsCrossAccountAttributes(awsCrossAccountAttributes).build();
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withAccountId(ACCOUNT_ID)
                                            .withCategory(SettingAttribute.SettingCategory.CE_CONNECTOR)
                                            .withValue(settingValue)
                                            .build();
    Mockito.doReturn(Arrays.asList(settingAttribute))
        .when(cloudToHarnessMappingService)
        .listSettingAttributesCreatedInDuration(any(), any(), any());

    RepeatStatus execute = awsECSClusterDataSyncTasklet.execute(null, chunkContext);
    assertThat(execute).isNull();
  }

  private CECluster getCeCluster() {
    return CECluster.builder()
        .accountId(ACCOUNT_ID)
        .clusterName("clusterName1")
        .region("us-east-1")
        .infraAccountId(AWS_ACCOUNT_ID)
        .infraMasterAccountId(AWS_MASTER_ACCOUNT_ID)
        .parentAccountSettingId(SETTING_ID)
        .build();
  }

  private AwsCrossAccountAttributes getAwsCrossAccountAttributes() {
    return AwsCrossAccountAttributes.builder().externalId(EXTERNAL_ID).crossAccountRoleArn(ROLE_ARN).build();
  }
}