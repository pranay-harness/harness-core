package software.wings.graphql.datafetcher.pipeline;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class PipelineDataFetcher extends AbstractDataFetcher<QLPipeline> {
  public static final String PIPELINE_ID_ARG = "pipelineId";

  @Inject HPersistence persistence;

  @Inject
  public PipelineDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLPipeline fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    String pipelineId = (String) getArgumentValue(dataFetchingEnvironment, PIPELINE_ID_ARG);

    Pipeline pipeline = persistence.get(Pipeline.class, pipelineId);
    if (pipeline == null) {
      throw new InvalidRequestException("Pipeline does not exist", WingsException.USER);
    }

    final QLPipelineBuilder builder = QLPipeline.builder();
    PipelineController.populatePipeline(pipeline, builder);
    return builder.build();
  }
}
