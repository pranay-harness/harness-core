package software.wings.search.entities.pipeline;

import io.harness.beans.EmbeddedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.search.framework.EntityBaseView;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "PipelineViewKeys")
public class PipelineView extends EntityBaseView {
  private String appId;
  private String appName;

  public PipelineView(String uuid, String name, String description, String accountId, long createdAt,
      long lastUpdatedAt, EntityType entityType, EmbeddedUser createdBy, EmbeddedUser lastUpdatedBy, String appId) {
    super(uuid, name, description, accountId, createdAt, lastUpdatedAt, entityType, createdBy, lastUpdatedBy);
    this.appId = appId;
  }

  public static PipelineView fromPipeline(Pipeline pipeline) {
    return new PipelineView(pipeline.getUuid(), pipeline.getName(), pipeline.getDescription(), pipeline.getAccountId(),
        pipeline.getCreatedAt(), pipeline.getLastUpdatedAt(), EntityType.PIPELINE, pipeline.getCreatedBy(),
        pipeline.getLastUpdatedBy(), pipeline.getAppId());
  }
}
