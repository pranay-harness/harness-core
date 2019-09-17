package software.wings.graphql.datafetcher.environment;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.tag.TagHelper;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTagFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTagType;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTypeFilter;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import java.util.List;
import java.util.Set;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
@Slf4j
public class EnvironmentQueryHelper {
  @Inject protected DataFetcherUtils utils;
  @Inject protected TagHelper tagHelper;

  public void setQuery(List<QLEnvironmentFilter> filters, Query query, String accountId) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Environment>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getEnvironment() != null) {
        field = query.field("_id");
        QLIdFilter environmentFilter = filter.getEnvironment();
        utils.setIdFilter(field, environmentFilter);
      }

      if (filter.getEnvironmentType() != null) {
        field = query.field("environmentType");
        QLEnvironmentTypeFilter envTypeFilter = filter.getEnvironmentType();
        utils.setEnumFilter(field, envTypeFilter);
      }

      if (filter.getTag() != null) {
        QLEnvironmentTagFilter environmentTagFilter = filter.getTag();
        List<QLTagInput> tags = environmentTagFilter.getTags();
        Set<String> entityIds =
            tagHelper.getEntityIdsFromTags(accountId, tags, getEntityType(environmentTagFilter.getEntityType()));
        switch (environmentTagFilter.getEntityType()) {
          case APPLICATION:
            query.field("appId").in(entityIds);
            break;
          default:
            logger.error("EntityType {} not supported in query", environmentTagFilter.getEntityType());
            throw new InvalidRequestException("Error while compiling query", WingsException.USER);
        }
      }
    });
  }

  public EntityType getEntityType(QLEnvironmentTagType entityType) {
    switch (entityType) {
      case APPLICATION:
        return EntityType.APPLICATION;
      default:
        logger.error("Unsupported entity type {} for tag ", entityType);
        throw new InvalidRequestException("Unsupported entity type " + entityType);
    }
  }
}
