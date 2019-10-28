package software.wings.search.entities;

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
      ExecutionArgs executionArgs, WorkflowType workflowType, ExecutionStatus executionStatus) {
    return WorkflowExecution.builder()
        .appId(appId)
        .appName(appName)
        .envType(NON_PROD)
        .status(executionStatus)
        .workflowType(workflowType)
        .executionArgs(executionArgs)
        .uuid(workflowExecutionId)
        .build();
  }

  private static DBObject getStatusChange() {
    BasicDBObject status = new BasicDBObject();
    status.put("status", ExecutionStatus.RUNNING);
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
    if (changeType.equals(ChangeType.UPDATE)) {
      changeEventBuilder = changeEventBuilder.changes(getStatusChange());
    }
    return changeEventBuilder.build();
  }
}
