package io.harness.perpetualtask;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.InstanceSyncConstants.INTERVAL_MINUTES;
import static software.wings.service.InstanceSyncConstants.TIMEOUT_SECONDS;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.instancesync.AwsAmiInstanceSyncPerpetualTaskParams;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.aws.model.AwsAsgListInstancesRequest;
import software.wings.service.impl.instance.InstanceSyncTestConstants;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AwsAmiInstanceSyncPerpetualTaskClientTest extends WingsBaseTest {
  @Mock PerpetualTaskService perpetualTaskService;
  @Mock SecretManager secretManager;
  @Mock SettingsService settingsService;
  @Mock InfrastructureMappingService infraMappingService;

  @InjectMocks @Inject AwsAmiInstanceSyncPerpetualTaskClient client;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void create() {
    client.create(ACCOUNT_ID,
        AwsAmiInstanceSyncPerpetualTaskClientParams.builder()
            .inframappingId(INFRA_MAPPING_ID)
            .appId(APP_ID)
            .asgName("asg")
            .build());

    verify(perpetualTaskService, Mockito.times(1))
        .createTask(PerpetualTaskType.AWS_AMI_INSTANCE_SYNC, ACCOUNT_ID, getClientContext(),
            PerpetualTaskSchedule.newBuilder()
                .setInterval(Durations.fromMinutes(INTERVAL_MINUTES))
                .setTimeout(Durations.fromSeconds(TIMEOUT_SECONDS))
                .build(),
            false);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getTaskParams() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).tag("tag").build();
    prepareTaskData(awsConfig);
    final AwsAmiInstanceSyncPerpetualTaskParams taskParams =
        (AwsAmiInstanceSyncPerpetualTaskParams) client.getTaskParams(getClientContext());

    assertThat(taskParams.getAsgName()).isEqualTo("asg");
    assertThat(taskParams.getAwsConfig()).isNotNull();
    assertThat(taskParams.getEncryptedData()).isNotNull();
    assertThat(taskParams.getRegion()).isEqualTo("us-east-1");
  }

  private PerpetualTaskClientContext getClientContext() {
    return new PerpetualTaskClientContext(ImmutableMap.of(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID,
        INFRA_MAPPING_ID, InstanceSyncConstants.HARNESS_APPLICATION_ID, APP_ID,
        AwsAmiInstanceSyncPerpetualTaskClient.ASG_NAME, "asg"));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getValidationTask() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).tag("tag").build();
    prepareTaskData(awsConfig);
    assertThat(client.getValidationTask(getClientContext(), ACCOUNT_ID))
        .isEqualTo(DelegateTask.builder()
                       .accountId(ACCOUNT_ID)
                       .appId(GLOBAL_APP_ID)
                       .tags(singletonList("tag"))
                       .data(TaskData.builder()
                                 .async(false)
                                 .taskType(TaskType.AWS_ASG_TASK.name())
                                 .parameters(new Object[] {AwsAsgListInstancesRequest.builder()
                                                               .awsConfig(awsConfig)
                                                               .encryptionDetails(new ArrayList<>())
                                                               .region("us-east-1")
                                                               .autoScalingGroupName("asg")
                                                               .build()})
                                 .timeout(TimeUnit.MINUTES.toMillis(InstanceSyncConstants.VALIDATION_TIMEOUT_MINUTES))
                                 .build())
                       .build());
  }

  private void prepareTaskData(AwsConfig awsConfig) {
    AwsAmiInfrastructureMapping infraMapping = new AwsAmiInfrastructureMapping();
    infraMapping.setRegion("us-east-1");
    infraMapping.setServiceId(InstanceSyncTestConstants.SERVICE_ID);
    infraMapping.setComputeProviderSettingId(InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID);

    doReturn(infraMapping).when(infraMappingService).get(APP_ID, INFRA_MAPPING_ID);
    doReturn(SettingAttribute.Builder.aSettingAttribute().withValue(awsConfig).build())
        .when(settingsService)
        .get(InstanceSyncTestConstants.COMPUTE_PROVIDER_SETTING_ID);
    doReturn(new ArrayList<>()).when(secretManager).getEncryptionDetails(any(AwsConfig.class));
  }
}