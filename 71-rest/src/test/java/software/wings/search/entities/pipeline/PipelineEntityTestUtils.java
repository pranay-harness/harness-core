package software.wings.search.entities.pipeline;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import software.wings.beans.Pipeline;
import software.wings.search.framework.changestreams.ChangeEvent;
import software.wings.search.framework.changestreams.ChangeEvent.ChangeEventBuilder;
import software.wings.search.framework.changestreams.ChangeType;

public class PipelineEntityTestUtils {
  public static Pipeline createPipeline(String accountId, String appId, String pipelineId, String pipelineName) {
    Pipeline pipeline = new Pipeline();
    pipeline.setUuid(pipelineId);
    pipeline.setAppId(appId);
    pipeline.setAccountId(accountId);
    pipeline.setName(pipelineName);
    return pipeline;
  }

  private static DBObject getPipelineChanges() {
    BasicDBObject basicDBObject = new BasicDBObject();
    basicDBObject.put("name", "edited_name");
    basicDBObject.put("appId", "appId");

    return basicDBObject;
  }

  public static ChangeEvent createPipelineChangeEvent(Pipeline pipeline, ChangeType changeType) {
    ChangeEventBuilder changeEventBuilder = ChangeEvent.builder();
    changeEventBuilder = changeEventBuilder.changeType(changeType)
                             .fullDocument(pipeline)
                             .token("token")
                             .uuid(pipeline.getUuid())
                             .entityType(Pipeline.class);

    if (changeType.equals(ChangeType.UPDATE)) {
      changeEventBuilder = changeEventBuilder.changes(getPipelineChanges());
    }

    return changeEventBuilder.build();
  }
}
