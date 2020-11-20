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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.service.impl.instance.InstanceSyncTestConstants;

import java.util.Collections;

public class AwsSshInstanceSyncPerpetualTaskCreatorTest extends WingsBaseTest {
  @Mock private PerpetualTaskService perpetualTaskService;
  @InjectMocks @Inject private AwsSshInstanceSyncPerpetualTaskCreator perpetualTaskController;

  @Before
  public void setup() {
    doReturn(PERPETUAL_TASK_ID)
        .when(perpetualTaskService)
        .createTask(eq(PerpetualTaskType.AWS_SSH_INSTANCE_SYNC), anyString(), any(), any(), eq(false), eq(""));
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
    verify(perpetualTaskService, Mockito.times(1))
        .createTask(eq(PerpetualTaskType.AWS_SSH_INSTANCE_SYNC), eq(InstanceSyncTestConstants.ACCOUNT_ID),
            eq(PerpetualTaskClientContext.builder()
                    .clientParams(ImmutableMap.of(InstanceSyncConstants.HARNESS_APPLICATION_ID, APP_ID,
                        InstanceSyncConstants.INFRASTRUCTURE_MAPPING_ID, INFRA_MAPPING_ID))
                    .build()),
            eq(PerpetualTaskSchedule.newBuilder()
                    .setInterval(Durations.fromMinutes(InstanceSyncConstants.INTERVAL_MINUTES))
                    .setTimeout(Durations.fromSeconds(InstanceSyncConstants.TIMEOUT_SECONDS))
                    .build()),
            eq(false), eq(""));
  }
}
