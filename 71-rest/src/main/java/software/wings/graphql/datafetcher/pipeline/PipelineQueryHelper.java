package software.wings.graphql.datafetcher.pipeline;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.tag.TagHelper;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineFilter;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineTagFilter;
import software.wings.graphql.schema.type.aggregation.pipeline.QLPipelineTagType;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import java.util.Set;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
@Slf4j
public class PipelineQueryHelper {
  @Inject protected DataFetcherUtils utils;
  @Inject protected TagHelper tagHelper;

  public void setQuery(List<QLPipelineFilter> filters, Query query, String accountId) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Pipeline>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getPipeline() != null) {
        field = query.field("_id");
        QLIdFilter pipelineFilter = filter.getPipeline();
        utils.setIdFilter(field, pipelineFilter);
      }

      if (filter.getTag() != null) {
        QLPipelineTagFilter pipelineTagFilter = filter.getTag();
        List<QLTagInput> tags = pipelineTagFilter.getTags();
        Set<String> entityIds =
            tagHelper.getEntityIdsFromTags(accountId, tags, getEntityType(pipelineTagFilter.getEntityType()));
        if (isNotEmpty(entityIds)) {
          switch (pipelineTagFilter.getEntityType()) {
            case APPLICATION:
              query.field("appId").in(entityIds);
              break;
            default:
              logger.error("EntityType {} not supported in query", pipelineTagFilter.getEntityType());
              throw new InvalidRequestException("Error while compiling query", WingsException.USER);
          }
        }
      }
    });
  }

  public EntityType getEntityType(QLPipelineTagType entityType) {
    switch (entityType) {
      case APPLICATION:
        return EntityType.APPLICATION;
      default:
        logger.error("Unsupported entity type {} for tag ", entityType);
        throw new InvalidRequestException("Unsupported entity type " + entityType);
    }
  }
}
