package software.wings.graphql.datafetcher.pipeline;

import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.user.UserController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;

public class PipelineController {
  public static void populatePipeline(Pipeline pipeline, QLPipelineBuilder builder) {
    builder.id(pipeline.getUuid())
        .name(pipeline.getName())
        .description(pipeline.getDescription())
        .createdAt(GraphQLDateTimeScalar.convert(pipeline.getCreatedAt()))
        .createdBy(UserController.populateUser(pipeline.getCreatedBy()));
  }
}
