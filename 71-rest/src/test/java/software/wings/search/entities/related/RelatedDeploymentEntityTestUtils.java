package software.wings.search.entities.related;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import software.wings.beans.WorkflowExecution;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeEvent.ChangeEventBuilder;
import software.wings.search.framework.changestreams.ChangeType;
import software.wings.sm.PipelineSummary;

import java.util.Arrays;

public class RelatedDeploymentEntityTestUtils {
  public static WorkflowExecution createWorkflowExecution(String workflowExecutionId, String appId, String serviceId,
      String envId, String workflowId, String pipelineId, WorkflowType workflowType) {
    WorkflowExecution workflowExecution = WorkflowExecution.builder()
                                              .appId(appId)
                                              .workflowId(workflowId)
                                              .workflowIds(Arrays.asList(workflowId))
                                              .serviceIds(Arrays.asList(serviceId))
                                              .envId(envId)
                                              .envIds(Arrays.asList(envId))
                                              .uuid(workflowExecutionId)
                                              .status(ExecutionStatus.RUNNING)
                                              .build();
    PipelineSummary pipelineSummary = PipelineSummary.builder().pipelineId(pipelineId).build();
    workflowExecution.setPipelineSummary(pipelineSummary);
    workflowExecution.setWorkflowType(workflowType);
    return workflowExecution;
  }

  public static DBObject createWorkflowExecutionChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("status", "RUNNING");
    basicDBObject.put("pipelineExecutionId", "pipelineExecutionId");

    return basicDBObject;
  }

  public static ChangeEvent createWorkflowExecutionChangeEvent(
      WorkflowExecution workflowExecution, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.token("token")
                             .uuid(generateUuid())
                             .fullDocument(workflowExecution)
                             .changeType(changeType)
                             .entityType(WorkflowExecution.class);

    if (changeType == ChangeType.UPDATE) {
      changeEventBuilder = changeEventBuilder.changes(createWorkflowExecutionChanges());
    }
    return changeEventBuilder.build();
  }
}
