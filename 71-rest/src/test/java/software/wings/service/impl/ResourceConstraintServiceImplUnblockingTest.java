package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.infra.InfraDefinitionTestConstants.RESOURCE_CONSTRAINT_NAME;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.distribution.constraint.Consumer.State;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceBuilder;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.states.HoldingScope;

public class ResourceConstraintServiceImplUnblockingTest extends WingsBaseTest {
  private static final String RESOURCE_CONSTRAINT_ID = generateUuid();
  private static final String WORKFLOW_EXECUTION_ID_1 = generateUuid();
  private static final String WORKFLOW_EXECUTION_ID_2 = generateUuid();
  private static final String WORKFLOW_EXECUTION_ID_3 = generateUuid();
  private static final String WORKFLOW_EXECUTION_ID_4 = generateUuid();
  private static final String INFRA_MAPPING_ID = generateUuid();
  @Mock private WorkflowExecutionService workflowExecutionService;

  @InjectMocks @Inject private ResourceConstraintService resourceConstraintService;

  private final ResourceConstraint resourceConstraint = ResourceConstraint.builder()
                                                            .uuid(RESOURCE_CONSTRAINT_ID)
                                                            .name(RESOURCE_CONSTRAINT_NAME)
                                                            .accountId(ACCOUNT_ID)
                                                            .capacity(4)
                                                            .strategy(Strategy.FIFO)
                                                            .build();

  WorkflowExecution workflowExecution1 = WorkflowExecution.builder()
                                             .appId(APP_ID)
                                             .uuid(WORKFLOW_EXECUTION_ID_1)
                                             .workflowId(WORKFLOW_ID)
                                             .workflowType(WorkflowType.ORCHESTRATION)
                                             .status(ExecutionStatus.SUCCESS)
                                             .build();

  WorkflowExecution workflowExecution2 = WorkflowExecution.builder()
                                             .appId(APP_ID)
                                             .uuid(WORKFLOW_EXECUTION_ID_2)
                                             .workflowId(WORKFLOW_ID)
                                             .workflowType(WorkflowType.ORCHESTRATION)
                                             .status(ExecutionStatus.RUNNING)
                                             .build();

  WorkflowExecution workflowExecution3 = WorkflowExecution.builder()
                                             .appId(APP_ID)
                                             .uuid(WORKFLOW_EXECUTION_ID_3)
                                             .workflowId(WORKFLOW_ID)
                                             .workflowType(WorkflowType.ORCHESTRATION)
                                             .status(ExecutionStatus.SUCCESS)
                                             .build();

  WorkflowExecution workflowExecution4 = WorkflowExecution.builder()
                                             .appId(APP_ID)
                                             .uuid(WORKFLOW_EXECUTION_ID_4)
                                             .workflowId(WORKFLOW_ID)
                                             .workflowType(WorkflowType.ORCHESTRATION)
                                             .status(ExecutionStatus.SUCCESS)
                                             .build();

  private final ResourceConstraintInstanceBuilder instanceBuilder =
      ResourceConstraintInstance.builder().appId(APP_ID).resourceConstraintId(RESOURCE_CONSTRAINT_ID).permits(1);

  private ResourceConstraintInstance instance1 = instanceBuilder.releaseEntityId(WORKFLOW_EXECUTION_ID_1)
                                                     .state(State.ACTIVE.name())
                                                     .resourceUnit(INFRA_MAPPING_ID)
                                                     .releaseEntityType(HoldingScope.WORKFLOW.name())
                                                     .order(1)
                                                     .build();

  private ResourceConstraintInstance instance2 = instanceBuilder.releaseEntityId(WORKFLOW_EXECUTION_ID_2)
                                                     .state(State.BLOCKED.name())
                                                     .resourceUnit(INFRA_MAPPING_ID)
                                                     .releaseEntityType(HoldingScope.WORKFLOW.name())
                                                     .order(2)
                                                     .build();

  private ResourceConstraintInstance instance3 = instanceBuilder.releaseEntityId(WORKFLOW_EXECUTION_ID_3)
                                                     .state(State.BLOCKED.name())
                                                     .resourceUnit(INFRA_MAPPING_ID)
                                                     .releaseEntityType(HoldingScope.WORKFLOW.name())
                                                     .order(3)
                                                     .build();

  private ResourceConstraintInstance instance4 = instanceBuilder.releaseEntityId(WORKFLOW_EXECUTION_ID_4)
                                                     .state(State.BLOCKED.name())
                                                     .resourceUnit(INFRA_MAPPING_ID)
                                                     .releaseEntityType(HoldingScope.WORKFLOW.name())
                                                     .order(4)
                                                     .build();
  @Before
  public void setup() throws Exception {
    wingsPersistence.save(resourceConstraint);
    wingsPersistence.save(instance1);
    wingsPersistence.save(instance2);
    wingsPersistence.save(instance3);
    wingsPersistence.save(instance4);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, WORKFLOW_EXECUTION_ID_1)).thenReturn(workflowExecution1);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, WORKFLOW_EXECUTION_ID_2)).thenReturn(workflowExecution2);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, WORKFLOW_EXECUTION_ID_3)).thenReturn(workflowExecution3);
    when(workflowExecutionService.getWorkflowExecution(APP_ID, WORKFLOW_EXECUTION_ID_4)).thenReturn(workflowExecution4);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testFinishAndUnblock() {
    boolean updated = resourceConstraintService.updateActiveConstraintForInstance(instance1);
    assertThat(updated).isEqualTo(true);
    ResourceConstraintInstance updatedInstance1 = queryInstance(APP_ID, instance1.getUuid());
    assertThat(updatedInstance1).isNotNull();
    assertThat(updatedInstance1.getState()).isEqualTo(State.FINISHED.name());

    ResourceConstraintInstance updatedInstance2 = queryInstance(APP_ID, instance2.getUuid());
    assertThat(updatedInstance2).isNotNull();
    assertThat(updatedInstance2.getState()).isEqualTo(State.ACTIVE.name());

    // These instances should go from blocked to finish
    ResourceConstraintInstance updatedInstance3 = queryInstance(APP_ID, instance3.getUuid());
    assertThat(updatedInstance3).isNotNull();
    assertThat(updatedInstance3.getState()).isEqualTo(State.FINISHED.name());

    ResourceConstraintInstance updatedInstance4 = queryInstance(APP_ID, instance4.getUuid());
    assertThat(updatedInstance4).isNotNull();
    assertThat(updatedInstance4.getState()).isEqualTo(State.FINISHED.name());
  }

  private ResourceConstraintInstance queryInstance(String appId, String uuid) {
    return wingsPersistence.createQuery(ResourceConstraintInstance.class)
        .filter(ResourceConstraintInstanceKeys.appId, appId)
        .filter(ResourceConstraintInstanceKeys.uuid, uuid)
        .get();
  }
}
