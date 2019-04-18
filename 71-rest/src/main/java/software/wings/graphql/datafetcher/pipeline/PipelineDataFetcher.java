package software.wings.graphql.datafetcher.pipeline;

import static software.wings.graphql.utils.GraphQLConstants.PIPELINE_TYPE;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.QLPipeline.QLPipelineBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class PipelineDataFetcher extends AbstractDataFetcher<QLPipeline> {
  @Inject HPersistence persistence;

  @Inject
  public PipelineDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  private static final PermissionAttribute permissionAttribute =
      new PermissionAttribute(PermissionType.PIPELINE, Action.READ);

  private boolean isAuthorizedToView(String appId, String pipelineId) {
    return isAuthorizedToView(appId, permissionAttribute, pipelineId);
  }

  @Override
  public QLPipeline fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    String pipelineId = (String) getArgumentValue(dataFetchingEnvironment, "pipelineId");

    Pipeline pipeline = persistence.get(Pipeline.class, pipelineId);

    if (!isAuthorizedToView(pipeline.getAppId(), pipelineId)) {
      throw notAuthorizedException(PIPELINE_TYPE, pipelineId, pipeline.getAppId());
    }

    final QLPipelineBuilder builder = QLPipeline.builder();
    PipelineController.populatePipeline(pipeline, builder);
    return builder.build();
  }
}
