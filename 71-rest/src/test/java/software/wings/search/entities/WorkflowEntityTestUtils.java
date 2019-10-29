package software.wings.search.entities;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import software.wings.beans.Account;
import software.wings.beans.Workflow;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeEvent.ChangeEventBuilder;
import software.wings.search.framework.changestreams.ChangeType;

public class WorkflowEntityTestUtils {
  public static Workflow createWorkflow(Account account, String appId, String workflowId, String workflowName) {
    Workflow workflow = new Workflow();
    workflow.setUuid(workflowId);
    workflow.setAppId(appId);
    workflow.setAccountId(account.getUuid());
    workflow.setName(workflowName);
    return workflow;
  }

  private static DBObject getWorkflowChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("name", "edited_name");
    return basicDBObject;
  }

  public static ChangeEvent createWorkflowChangeEvent(Workflow environment, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(environment)
                             .token("token")
                             .uuid(environment.getUuid())
                             .entityType(Workflow.class);

    if (changeType.equals(ChangeType.UPDATE)) {
      changeEventBuilder = changeEventBuilder.changes(getWorkflowChanges());
    }

    return changeEventBuilder.build();
  }
}
