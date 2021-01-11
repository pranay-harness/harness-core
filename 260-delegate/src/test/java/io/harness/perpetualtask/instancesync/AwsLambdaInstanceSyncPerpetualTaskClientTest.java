package io.harness.perpetualtask.instancesync;

import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.InstanceSyncConstants;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;
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
public class AwsLambdaInstanceSyncPerpetualTaskClientTest extends WingsBaseTest {
  @Mock SecretManager secretManager;
  @Mock SettingsService settingsService;
  @Mock InfrastructureMappingService infraMappingService;
  private final long startDate = 1590653005287l;

  @InjectMocks @Inject AwsLambdaInstanceSyncPerpetualTaskClient client;

  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void getTaskParams() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).tag("tag").build();
    prepareTaskData(awsConfig);
    final AwsLambdaInstanceSyncPerpetualTaskParams taskParams =
        (AwsLambdaInstanceSyncPerpetualTaskParams) client.getTaskParams(getClientContext());

    assertThat(taskParams.getAwsConfig()).isNotNull();
    assertThat(taskParams.getEncryptedData()).isNotNull();
    assertThat(taskParams.getRegion()).isEqualTo("us-east-1");
    assertThat(taskParams.getFunctionName()).isEqualTo("function");
    assertThat(taskParams.getQualifier()).isEqualTo("version");
  }
  @Test
  @Owner(developers = OwnerRule.ACASIAN)
  @Category(UnitTests.class)
  public void getValidationTask() {
    AwsConfig awsConfig = AwsConfig.builder().accountId(ACCOUNT_ID).tag("tag").build();
    prepareTaskData(awsConfig);
    AwsLambdaDetailsRequest request = AwsLambdaDetailsRequest.builder()
                                          .awsConfig(awsConfig)
                                          .encryptionDetails(emptyList())
                                          .region("us-east-1")
                                          .functionName("function")
                                          .qualifier("version")
                                          .loadAliases(true)
                                          .build();

    final DelegateTask validationTask = client.getValidationTask(getClientContext(), ACCOUNT_ID);
    assertThat(validationTask)
        .isEqualTo(DelegateTask.builder()
                       .accountId(ACCOUNT_ID)
                       .tags(singletonList("tag"))
                       .data(TaskData.builder()
                                 .async(false)
                                 .taskType(TaskType.AWS_LAMBDA_TASK.name())
                                 .parameters(new Object[] {request})
                                 .timeout(validationTask.getData().getTimeout())
                                 .build())
                       .build());

    assertThat(validationTask.getData().getTimeout())
        .isLessThanOrEqualTo(System.currentTimeMillis() + TaskData.DELEGATE_QUEUE_TIMEOUT);
    assertThat(validationTask.getData().getTimeout()).isGreaterThanOrEqualTo(System.currentTimeMillis());
  }

  private PerpetualTaskClientContext getClientContext() {
    return PerpetualTaskClientContext.builder()
        .clientParams(ImmutableMap.of(InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, INFRA_MAPPING_ID,
            InstanceSyncConstants.HARNESS_APPLICATION_ID, APP_ID,
            AwsLambdaInstanceSyncPerpetualTaskClient.FUNCTION_NAME, "function",
            AwsLambdaInstanceSyncPerpetualTaskClient.QUALIFIER, "version",
            AwsLambdaInstanceSyncPerpetualTaskClient.START_DATE, String.valueOf(startDate)))
        .build();
  }

  private void prepareTaskData(AwsConfig awsConfig) {
    AwsLambdaInfraStructureMapping infraMapping = new AwsLambdaInfraStructureMapping();
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
