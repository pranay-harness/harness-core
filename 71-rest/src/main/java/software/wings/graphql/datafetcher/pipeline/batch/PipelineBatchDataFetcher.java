package software.wings.graphql.datafetcher.pipeline.batch;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.DataLoader;
import software.wings.graphql.datafetcher.AbstractBatchDataFetcher;
import software.wings.graphql.schema.query.QLPipelineQueryParameters;
import software.wings.graphql.schema.type.QLPipeline;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.concurrent.CompletionStage;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PipelineBatchDataFetcher extends AbstractBatchDataFetcher<QLPipeline, QLPipelineQueryParameters, String> {
  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected CompletionStage<QLPipeline> load(
      QLPipelineQueryParameters parameters, DataLoader<String, QLPipeline> dataLoader) {
    final String pipelineId;
    if (StringUtils.isNotBlank(parameters.getPipelineId())) {
      pipelineId = parameters.getPipelineId();
    } else {
      throw new InvalidRequestException("Pipeline Id not present in query", WingsException.USER);
    }
    return dataLoader.load(pipelineId);
  }
}