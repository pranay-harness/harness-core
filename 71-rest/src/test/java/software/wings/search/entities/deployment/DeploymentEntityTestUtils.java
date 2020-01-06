package software.wings.search.entities.deployment;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.persistence.PersistentEntity;
import software.wings.WingsBaseTest;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.WorkflowExecution;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeEvent.ChangeEventBuilder;
import software.wings.search.framework.changestreams.ChangeType;
import software.wings.sm.PipelineSummary;

import java.util.Arrays;

public class DeploymentEntityTestUtils extends WingsBaseTest {
  public static ExecutionArgs createExecutionArgs(WorkflowType workflowType) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflowType);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(generateUuid());

    return executionArgs;
  }

  public static WorkflowExecution createWorkflowExecution(String workflowExecutionId, String appId, String appName,
      String envId, String serviceId, String workflowId, String pipelineId, ExecutionArgs executionArgs,
      WorkflowType workflowType, ExecutionStatus executionStatus) {
    WorkflowExecution workflowExecution = WorkflowExecution.builder()
                                              .appId(appId)
                                              .appName(appName)
                                              .envType(NON_PROD)
                                              .status(executionStatus)
                                              .workflowType(workflowType)
                                              .executionArgs(executionArgs)
                                              .uuid(workflowExecutionId)
                                              .build();

    workflowExecution.setServiceIds(Arrays.asList(serviceId));
    workflowExecution.setEnvIds(Arrays.asList(envId));
    workflowExecution.setWorkflowIds(Arrays.asList(workflowId));
    workflowExecution.setWorkflowId(workflowId);
    PipelineSummary pipelineSummary =
        PipelineSummary.builder().pipelineId(pipelineId).pipelineName("pipeline_name").build();

    workflowExecution.setPipelineSummary(pipelineSummary);

    return workflowExecution;
  }

  private static DBObject getStatusChange() {
    BasicDBObject status = new BasicDBObject();
    status.put("status", ExecutionStatus.RUNNING);
    status.put("workflowId", generateUuid());
    return status;
  }

  public static <T extends PersistentEntity> ChangeEvent<T> createWorkflowExecutionChangeEvent(
      Class<T> entityType, T fullDocument, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.uuid(generateUuid())
                             .token("token")
                             .fullDocument(fullDocument)
                             .entityType(entityType)
                             .changeType(changeType);
    if (changeType == ChangeType.UPDATE) {
      changeEventBuilder = changeEventBuilder.changes(getStatusChange());
    }
    return changeEventBuilder.build();
  }
}
