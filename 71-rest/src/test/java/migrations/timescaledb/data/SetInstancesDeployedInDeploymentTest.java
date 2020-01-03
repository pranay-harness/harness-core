package migrations.timescaledb.data;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAMA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author rktummala on 10/16/19
 */
public class SetInstancesDeployedInDeploymentTest extends WingsBaseTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @Mock ResultSet resultSet;
  @Inject WorkflowService workflowService;
  @Spy @Inject @InjectMocks SetInstancesDeployedInDeployment setInstancesDeployedInDeployment;

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testMigrate() throws SQLException {
    String workflowExecutionId = generateUuid();
    WorkflowExecution workflowExecution = createWorkflowExecution(workflowExecutionId);
    wingsPersistence.save(workflowExecution);

    when(timeScaleDBService.isValid()).thenReturn(true);
    Connection connection = mock(Connection.class);
    when(timeScaleDBService.getDBConnection()).thenReturn(connection);
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(resultSet.next()).thenReturn(true, false);
    when(preparedStatement.executeQuery()).thenReturn(resultSet);

    boolean migrated = setInstancesDeployedInDeployment.migrate();
    assertThat(migrated).isTrue();
  }

  @NotNull
  private WorkflowExecution createWorkflowExecution(String workflowExecutionId) {
    StateMachine sm = new StateMachine();
    sm.setAppId(APP_ID);
    sm = workflowService.createStateMachine(sm);
    WorkflowExecution workflowExecution = WorkflowExecution.builder().build();
    workflowExecution.setCreatedAt(System.currentTimeMillis() - 100000);
    workflowExecution.setStartTs(System.currentTimeMillis() - 50000);
    workflowExecution.setEndTs(System.currentTimeMillis() - 30000);
    workflowExecution.setStatus(ExecutionStatus.SUCCESS);
    workflowExecution.setAccountId(ACCOUNT_ID);
    workflowExecution.setAppId(sm.getAppId());
    workflowExecution.setUuid(workflowExecutionId);
    workflowExecution.setStateMachine(sm);
    return workflowExecution;
  }
}