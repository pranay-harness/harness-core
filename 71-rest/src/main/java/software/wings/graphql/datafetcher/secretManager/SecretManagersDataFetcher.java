package software.wings.graphql.datafetcher.secretManager;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.SecretManagerConfig.SecretManagerConfigKeys;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager.QLSecretManagerBuilder;
import software.wings.graphql.schema.type.secretManagers.QLSecretManagerConnection;
import software.wings.graphql.schema.type.secretManagers.QLSecretManagerConnection.QLSecretManagerConnectionBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.List;

public class SecretManagersDataFetcher
    extends AbstractConnectionV2DataFetcher<QLSecretManagerFilter, QLNoOpSortCriteria, QLSecretManagerConnection> {
  @Inject protected DataFetcherUtils dataFetcherUtils;

  @Override
  protected QLSecretManagerFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return QLSecretManagerFilter.builder().build();
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLSecretManagerConnection fetchConnection(List<QLSecretManagerFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<SecretManagerConfig> query = populateFilters(wingsPersistence, filters, SecretManagerConfig.class, true);
    query.order(Sort.descending(SecretManagerConfig.CREATED_AT_KEY));
    QLSecretManagerConnectionBuilder connectionBuilder = QLSecretManagerConnection.builder();
    connectionBuilder.pageInfo(dataFetcherUtils.populate(pageQueryParameters, query, secretManager -> {
      QLSecretManagerBuilder builder = QLSecretManager.builder();
      SecretManagerController.populateSecretManager(secretManager, builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLSecretManagerFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }
    filters.forEach(filter -> {
      FieldEnd<? extends Query<SettingAttribute>> field;
      if (filter.getSecretManager() != null) {
        field = query.field("_id");
        QLIdFilter secretManagerFilter = filter.getSecretManager();
        dataFetcherUtils.setIdFilter(field, secretManagerFilter);
      }
      if (filter.getType() != null) {
        QLSecretManagerTypeFilter entityTypeFilter = filter.getType();
        field = query.field(SecretManagerConfigKeys.encryptionType);
        dataFetcherUtils.setEnumFilter(field, entityTypeFilter);
      }
    });
  }
}
