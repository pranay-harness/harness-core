package software.wings.graphql.datafetcher.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.datafetcher.tag.TagHelper;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceTagFilter;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceTagType;
import software.wings.graphql.schema.type.aggregation.tag.QLTagInput;
import software.wings.graphql.schema.type.instance.QLInstanceType;

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
@Singleton
@Slf4j
public class InstanceQueryHelper {
  @Inject protected DataFetcherUtils utils;
  @Inject protected TagHelper tagHelper;

  public void setQuery(String accountId, List<QLInstanceFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Instance>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getCloudProvider() != null) {
        field = query.field("computeProviderId");
        QLIdFilter cloudProviderFilter = filter.getCloudProvider();
        utils.setIdFilter(field, cloudProviderFilter);
      }

      if (filter.getEnvironment() != null) {
        field = query.field("envId");
        QLIdFilter envFilter = filter.getEnvironment();
        utils.setIdFilter(field, envFilter);
      }

      if (filter.getService() != null) {
        field = query.field("serviceId");
        QLIdFilter serviceFilter = filter.getService();
        utils.setIdFilter(field, serviceFilter);
      }

      if (filter.getCreatedAt() != null) {
        field = query.field("createdAt");
        QLTimeFilter createdAtFilter = filter.getCreatedAt();
        utils.setTimeFilter(field, createdAtFilter);
      }

      if (filter.getInstanceType() != null) {
        field = query.field("instanceType");
        QLInstanceType instanceTypeFilter = filter.getInstanceType();
        utils.setStringFilter(field,
            QLStringFilter.builder()
                .operator(QLStringOperator.EQUALS)
                .values(new String[] {instanceTypeFilter.name()})
                .build());
      }

      if (filter.getTag() != null) {
        QLInstanceTagFilter tagFilter = filter.getTag();
        List<QLTagInput> tags = tagFilter.getTags();
        Set<String> entityIds =
            tagHelper.getEntityIdsFromTags(accountId, tags, getEntityType(tagFilter.getEntityType()));
        switch (tagFilter.getEntityType()) {
          case APPLICATION:
            query.field("appId").in(entityIds);
            break;
          case SERVICE:
            query.field("serviceId").in(entityIds);
            break;
          case ENVIRONMENT:
            query.field("envId").in(entityIds);
            break;
          default:
            log.error("EntityType {} not supported in query", tagFilter.getEntityType());
            throw new InvalidRequestException("Error while compiling query", WingsException.USER);
        }
      }
    });
  }

  public EntityType getEntityType(QLInstanceTagType entityType) {
    switch (entityType) {
      case APPLICATION:
        return EntityType.APPLICATION;
      case SERVICE:
        return EntityType.SERVICE;
      case ENVIRONMENT:
        return EntityType.ENVIRONMENT;
      default:
        log.error("Unsupported entity type {} for tag ", entityType);
        throw new InvalidRequestException("Unsupported entity type " + entityType);
    }
  }
}
