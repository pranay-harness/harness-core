package software.wings.graphql.datafetcher.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.PipelineKeys;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLPipelineQueryParameters;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@OwnedBy(CDC)
@Slf4j
public class PipelineDataFetcher extends AbstractObjectDataFetcher<QLPipeline, QLPipelineQueryParameters> {
  public static final String PIPELINE_DOES_NOT_EXIST_MSG = "Pipeline does not exist";
  public static final String EMPTY_PIPELINE_NAME = "Empty Pipeline name";
  public static final String EMPTY_APPLICATION_ID = "Empty Application Id";

  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.PIPELINE, action = Action.READ)
  public QLPipeline fetch(QLPipelineQueryParameters qlQuery, String accountId) {
    Pipeline pipeline = null;

    if (qlQuery.getPipelineId() != null) {
      pipeline = persistence.get(Pipeline.class, qlQuery.getPipelineId());
    } else if (qlQuery.getPipelineName() != null) {
      if (EmptyPredicate.isEmpty(qlQuery.getApplicationId())) {
        throw new InvalidRequestException(EMPTY_APPLICATION_ID, WingsException.USER);
      }

      if (EmptyPredicate.isEmpty(qlQuery.getPipelineName())) {
        throw new InvalidRequestException(EMPTY_PIPELINE_NAME, WingsException.USER);
      }

      pipeline = persistence.createQuery(Pipeline.class)
                     .filter(PipelineKeys.appId, qlQuery.getApplicationId())
                     .filter(PipelineKeys.name, qlQuery.getPipelineName())
                     .get();
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
      return null;
    }

    if (!pipeline.getAccountId().equals(accountId)) {
      throw new InvalidRequestException(PIPELINE_DOES_NOT_EXIST_MSG, WingsException.USER);
    }

    final QLPipelineBuilder builder = QLPipeline.builder();
    PipelineController.populatePipeline(pipeline, builder);
    return builder.build();
  }
}
