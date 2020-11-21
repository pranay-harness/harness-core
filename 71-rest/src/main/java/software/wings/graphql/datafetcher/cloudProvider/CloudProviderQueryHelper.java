package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilter;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderTypeFilter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
public class CloudProviderQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLCloudProviderFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<SettingAttribute>> field;

      if (filter.getCloudProvider() != null) {
        field = query.field("_id");
        QLIdFilter cloudProviderFilter = filter.getCloudProvider();
        utils.setIdFilter(field, cloudProviderFilter);
      }

      if (filter.getCloudProviderType() != null) {
        field = query.field("value.type");
        QLCloudProviderTypeFilter cloudProviderTypeFilter = filter.getCloudProviderType();
        utils.setEnumFilter(field, cloudProviderTypeFilter);
      }

      if (filter.getCreatedAt() != null) {
        field = query.field("createdAt");
        QLTimeFilter createdAtFilter = filter.getCreatedAt();
        utils.setTimeFilter(field, createdAtFilter);
      }
    });
  }
}
