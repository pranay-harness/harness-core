package software.wings.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.PERPETUAL_TASK_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.AwsSshPTClientParams;
import io.harness.perpetualtask.instancesync.AwsSshPerpetualTaskServiceClient;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsInfrastructureMapping;

import java.util.Collections;

public class AwsSshInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock private AwsSshPerpetualTaskServiceClient perpetualTaskServiceClient;
  @InjectMocks @Inject private AwsSshInstanceSyncPerpetualTaskCreator perpetualTaskController;

  @Before
  public void setup() {
    doReturn(PERPETUAL_TASK_ID).when(perpetualTaskServiceClient).create(anyString(), any());
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void createPerpetualTasks() {
    perpetualTaskController.createPerpetualTasks(getInfraMapping());
    verifyCreatePerpetualTaskInternal();
  }

  private AwsInfrastructureMapping getInfraMapping() {
    AwsInfrastructureMapping infrastructureMapping = new AwsInfrastructureMapping();
    infrastructureMapping.setAccountId(ACCOUNT_ID);
    infrastructureMapping.setAppId(APP_ID);
    infrastructureMapping.setUuid(INFRA_MAPPING_ID);
    return infrastructureMapping;
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void createPerpetualTasksForNewDeployment() {
    perpetualTaskController.createPerpetualTasksForNewDeployment(
        Collections.emptyList(), Collections.emptyList(), getInfraMapping());

    verifyCreatePerpetualTaskInternal();
  }

  private void verifyCreatePerpetualTaskInternal() {
    verify(perpetualTaskServiceClient, Mockito.times(1))
        .create(eq(ACCOUNT_ID),
            Matchers.eq(AwsSshPTClientParams.builder().appId(APP_ID).inframappingId(INFRA_MAPPING_ID).build()));
  }
}