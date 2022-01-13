/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance;

import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.ANKIT;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.api.DeploymentSummary;
import software.wings.beans.InfrastructureMapping;
import software.wings.dl.WingsPersistence;
import software.wings.service.InstanceSyncPerpetualTaskCreator;
import software.wings.service.impl.instance.InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys;
import software.wings.service.intfc.instance.InstanceService;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.Collections;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class InstanceSyncPerpetualTaskServiceImplTest extends WingsBaseTest {
  private static final String PERPETUAL_TASK_ID_1 = "DummyTaskId1";
  private static final String PERPETUAL_TASK_ID_2 = "DummyTaskId2";

  @Mock private PerpetualTaskService perpetualTaskService;
  @Mock private InstanceService instanceService;
  @Mock private InstanceHandlerFactory instanceHandlerFactory;
  @Inject WingsPersistence wingsPersistence;
  @InjectMocks private InstanceSyncPerpetualTaskServiceImpl instanceSyncPerpetualTaskService;
  private InfrastructureMapping infrastructureMapping;
  private InstanceSyncPerpetualTaskCreator perpetualTaskCreator;

  @Before
  public void setUp() throws IllegalAccessException {
    FieldUtils.writeField(instanceSyncPerpetualTaskService, "wingsPersistence", wingsPersistence, true);

    infrastructureMapping = mock(InfrastructureMapping.class);
    when(infrastructureMapping.getAccountId()).thenReturn(ACCOUNT_ID);
    when(infrastructureMapping.getAppId()).thenReturn(APP_ID);
    when(infrastructureMapping.getUuid()).thenReturn(INFRA_MAPPING_ID);

    InstanceHandler instanceHandler =
        mock(InstanceHandler.class, withSettings().extraInterfaces(InstanceSyncByPerpetualTaskHandler.class));
    perpetualTaskCreator = mock(InstanceSyncPerpetualTaskCreator.class);

    when(instanceHandlerFactory.getInstanceHandler(infrastructureMapping)).thenReturn(instanceHandler);
    when(((InstanceSyncByPerpetualTaskHandler) instanceHandler).getInstanceSyncPerpetualTaskCreator())
        .thenReturn(perpetualTaskCreator);

    when(perpetualTaskService.deleteTask(ACCOUNT_ID, PERPETUAL_TASK_ID_1)).thenReturn(true);

    wingsPersistence.delete(wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class, excludeAuthority));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void createPerpetualTasks() {
    when(instanceService.getInstanceCount(APP_ID, INFRA_MAPPING_ID)).thenReturn(1L);
    when(perpetualTaskCreator.createPerpetualTasks(infrastructureMapping))
        .thenReturn(Collections.singletonList(PERPETUAL_TASK_ID_1));

    instanceSyncPerpetualTaskService.createPerpetualTasks(infrastructureMapping);

    verify(perpetualTaskCreator, times(1)).createPerpetualTasks(infrastructureMapping);
    InstanceSyncPerpetualTaskInfo info =
        wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class)
            .filter(InstanceSyncPerpetualTaskInfoKeys.accountId, ACCOUNT_ID)
            .filter(InstanceSyncPerpetualTaskInfoKeys.infrastructureMappingId, INFRA_MAPPING_ID)
            .get();
    assertEquals(ACCOUNT_ID, info.getAccountId());
    assertEquals(INFRA_MAPPING_ID, info.getInfrastructureMappingId());
    assertEquals(1, info.getPerpetualTaskIds().size());
    assertEquals("DummyTaskId1", info.getPerpetualTaskIds().get(0));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void deletePerpetualTasks() {
    when(instanceService.getInstanceCount(APP_ID, INFRA_MAPPING_ID)).thenReturn(1L);
    when(perpetualTaskCreator.createPerpetualTasks(infrastructureMapping))
        .thenReturn(Collections.singletonList(PERPETUAL_TASK_ID_1));

    instanceSyncPerpetualTaskService.createPerpetualTasks(infrastructureMapping);

    assertEquals(1,
        wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class)
            .filter(InstanceSyncPerpetualTaskInfoKeys.accountId, ACCOUNT_ID)
            .count());

    instanceSyncPerpetualTaskService.deletePerpetualTasks(infrastructureMapping);

    verify(perpetualTaskService, times(1)).deleteTask(ACCOUNT_ID, PERPETUAL_TASK_ID_1);
    assertEquals(0,
        wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class)
            .filter(InstanceSyncPerpetualTaskInfoKeys.accountId, ACCOUNT_ID)
            .count());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void resetPerpetualTask() {
    instanceSyncPerpetualTaskService.resetPerpetualTask(ACCOUNT_ID, PERPETUAL_TASK_ID_1);
    verify(perpetualTaskService, times(1)).resetTask(ACCOUNT_ID, PERPETUAL_TASK_ID_1, null);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void createPerpetualTasksForNewDeployment() {
    when(instanceService.getInstanceCount(APP_ID, INFRA_MAPPING_ID)).thenReturn(1L);
    when(perpetualTaskCreator.createPerpetualTasks(infrastructureMapping))
        .thenReturn(Collections.singletonList(PERPETUAL_TASK_ID_1));

    instanceSyncPerpetualTaskService.createPerpetualTasks(infrastructureMapping);

    ImmutableList<DeploymentSummary> deploymentSummaries = ImmutableList.of(mock(DeploymentSummary.class));
    PerpetualTaskRecord record = mock(PerpetualTaskRecord.class);
    when(perpetualTaskService.getTaskRecord(PERPETUAL_TASK_ID_1)).thenReturn(record);
    when(perpetualTaskCreator.createPerpetualTasksForNewDeployment(
             deploymentSummaries, ImmutableList.of(record), infrastructureMapping))
        .thenReturn(ImmutableList.of(PERPETUAL_TASK_ID_2));

    instanceSyncPerpetualTaskService.createPerpetualTasksForNewDeployment(infrastructureMapping, deploymentSummaries);

    InstanceSyncPerpetualTaskInfo info =
        wingsPersistence.createQuery(InstanceSyncPerpetualTaskInfo.class)
            .filter(InstanceSyncPerpetualTaskInfoKeys.accountId, ACCOUNT_ID)
            .filter(InstanceSyncPerpetualTaskInfoKeys.infrastructureMappingId, INFRA_MAPPING_ID)
            .get();
    assertEquals(ACCOUNT_ID, info.getAccountId());
    assertEquals(INFRA_MAPPING_ID, info.getInfrastructureMappingId());
    assertEquals(2, info.getPerpetualTaskIds().size());
    assertEquals("DummyTaskId1", info.getPerpetualTaskIds().get(0));
    assertEquals("DummyTaskId2", info.getPerpetualTaskIds().get(1));
  }
}
