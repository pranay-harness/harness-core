package software.wings.graphql.datafetcher.pipeline;

import lombok.experimental.UtilityClass;
import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;

@UtilityClass
public class PipelineController {
  public static void populatePipeline(Pipeline pipeline, QLPipelineBuilder builder) {
    builder.id(pipeline.getUuid())
        .name(pipeline.getName())
        .appId(pipeline.getAppId())
        .description(pipeline.getDescription())
        .createdAt(pipeline.getCreatedAt())
        .createdBy(UserController.populateUser(pipeline.getCreatedBy()));
  }
}
