package io.harness.perpetualtask;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.instancesync.AwsAmiInstanceSyncPerpetualTaskParams;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import io.harness.tasks.Cd1SetupFields;

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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AwsAmiInstanceSyncPerpetualTaskClientTest extends WingsBaseTest {
  @Mock SecretManager secretManager;
  @Mock SettingsService settingsService;
  @Mock InfrastructureMappingService infraMappingService;

  @InjectMocks @Inject AwsAmiInstanceSyncPerpetualTaskClient client;

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
    return PerpetualTaskClientContext.builder()
        .clientParams(ImmutableMap.of(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, INFRA_MAPPING_ID,
            InstanceSyncConstants.HARNESS_APPLICATION_ID, APP_ID, AwsAmiInstanceSyncPerpetualTaskClient.ASG_NAME,
            "asg"))
        .build();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getValidationTask() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).tag("tag").build();
    prepareTaskData(awsConfig);
    DelegateTask validationTask = client.getValidationTask(getClientContext(), ACCOUNT_ID);
    assertThat(validationTask)
        .isEqualTo(DelegateTask.builder()
                       .accountId(ACCOUNT_ID)
                       .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, GLOBAL_APP_ID)
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
                                 .timeout(validationTask.getData().getTimeout())
                                 .build())
                       .build());
    assertThat(validationTask.getData().getTimeout())
        .isLessThanOrEqualTo(System.currentTimeMillis() + TaskData.DELEGATE_QUEUE_TIMEOUT);
    assertThat(validationTask.getData().getTimeout()).isGreaterThanOrEqualTo(System.currentTimeMillis());
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
