package software.wings.graphql.datafetcher.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
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
