package software.wings.graphql.datafetcher.pipeline;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Pipeline;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLPipelineQueryParameters;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class PipelineDataFetcher extends AbstractDataFetcher<QLPipeline, QLPipelineQueryParameters> {
  @Inject HPersistence persistence;

  @Inject
  public PipelineDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  public QLPipeline fetch(QLPipelineQueryParameters qlQuery) {
    Pipeline pipeline = null;
    if (qlQuery.getPipelineId() != null) {
      pipeline = persistence.get(Pipeline.class, qlQuery.getPipelineId());
    } else if (qlQuery.getExecutionId() != null) {
      // TODO: add this to in memory cache
      final String pipelineId = persistence.createQuery(WorkflowExecution.class)
                                    .filter(WorkflowExecutionKeys.uuid, qlQuery.getExecutionId())
                                    .project(WorkflowExecutionKeys.workflowId, true)
                                    .get()
                                    .getWorkflowId();

      pipeline = persistence.get(Pipeline.class, pipelineId);
    }

    if (pipeline == null) {
      throw new InvalidRequestException("Pipeline does not exist", WingsException.USER);
    }

    final QLPipelineBuilder builder = QLPipeline.builder();
    PipelineController.populatePipeline(pipeline, builder);
    return builder.build();
  }
}
