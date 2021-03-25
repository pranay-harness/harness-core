package software.wings.graphql.datafetcher.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.beans.EntityType;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.tag.TagHelper;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.service.QLServiceFilter;
import software.wings.graphql.schema.type.aggregation.service.QLServiceTagFilter;
import software.wings.graphql.schema.type.aggregation.service.QLServiceTagType;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;

/**
 * @author rktummala on 07/12/19
 */
@OwnedBy(CDC)
@Singleton
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ServiceQueryHelper {
  @Inject private DataFetcherUtils utils;
  @Inject private TagHelper tagHelper;

  public void setQuery(List<QLServiceFilter> filters, Query query, String accountId) {
    if (hasNone(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Service>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getService() != null) {
        field = query.field("_id");
        QLIdFilter serviceFilter = filter.getService();
        utils.setIdFilter(field, serviceFilter);
      }

      if (filter.getTag() != null) {
        QLServiceTagFilter serviceTagFilter = filter.getTag();
        List<QLTagInput> tags = serviceTagFilter.getTags();
        Set<String> entityIds =
            tagHelper.getEntityIdsFromTags(accountId, tags, getEntityType(serviceTagFilter.getEntityType()));
        switch (serviceTagFilter.getEntityType()) {
          case APPLICATION:
            query.field("appId").in(entityIds);
            break;
          default:
            log.error("EntityType {} not supported in query", serviceTagFilter.getEntityType());
            throw new InvalidRequestException("Error while compiling query", WingsException.USER);
        }
      }
    });
  }

  public EntityType getEntityType(QLServiceTagType entityType) {
    switch (entityType) {
      case APPLICATION:
        return EntityType.APPLICATION;
      default:
        log.error("Unsupported entity type {} for tag ", entityType);
        throw new InvalidRequestException("Unsupported entity type " + entityType);
    }
  }
}
