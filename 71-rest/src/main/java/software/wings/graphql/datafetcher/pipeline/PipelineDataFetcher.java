package software.wings.graphql.datafetcher.pipeline;

import static software.wings.graphql.utils.GraphQLConstants.PIPELINE_TYPE;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.graphql.schema.type.WorkflowInfo;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class PipelineDataFetcher extends AbstractDataFetcher<WorkflowInfo> {
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
  public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    String pipelineId = (String) getArgumentValue(dataFetchingEnvironment, "pipelineId");

    Pipeline pipeline = persistence.get(Pipeline.class, pipelineId);

    if (!isAuthorizedToView(pipeline.getAppId(), pipelineId)) {
      throwNotAuthorizedException(PIPELINE_TYPE, pipelineId, pipeline.getAppId());
    }

    return QLPipeline.builder()
        .id(pipeline.getUuid())
        .name(pipeline.getName())
        .description(pipeline.getDescription())
        .build();
  }
}
