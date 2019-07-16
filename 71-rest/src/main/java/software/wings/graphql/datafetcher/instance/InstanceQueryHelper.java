package software.wings.graphql.datafetcher.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.schema.type.instance.QLInstanceType;

import java.util.List;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
public class InstanceQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLInstanceFilter> filters, Query query) {
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
    });
  }
}
