package software.wings.graphql.datafetcher.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.exception.WingsException;

import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLInstanceConnection;
import software.wings.graphql.schema.type.QLInstanceConnection.QLInstanceConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.schema.type.instance.QLInstance;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

@Slf4j
public class InstanceConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLInstanceFilter, QLNoOpSortCriteria, QLInstanceConnection> {
  @Inject private InstanceControllerManager instanceControllerManager;
  @Inject private InstanceQueryHelper instanceMongoHelper;

  @Override
  @AuthRule(permissionType = LOGGED_IN)
  protected QLInstanceConnection fetchConnection(List<QLInstanceFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Instance> query = populateFilters(wingsPersistence, filters, Instance.class, true)
                                .filter(InstanceKeys.isDeleted, false)
                                .order(Sort.descending(InstanceKeys.lastDeployedAt));

    QLInstanceConnectionBuilder connectionBuilder = QLInstanceConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, instance -> {
      QLInstance qlInstance = instanceControllerManager.getQLInstance(instance);
      connectionBuilder.node(qlInstance);
    }));

    return connectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLInstanceFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }
    instanceMongoHelper.setQuery(getAccountId(), filters, query);
  }

  @Override
  protected QLInstanceFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    QLIdFilter idFilter = QLIdFilter.builder()
                              .operator(QLIdOperator.EQUALS)
                              .values(new String[] {(String) utils.getFieldValue(environment.getSource(), value)})
                              .build();
    if (NameService.application.equals(key)) {
      return QLInstanceFilter.builder().application(idFilter).build();
    } else if (NameService.service.equals(key)) {
      return QLInstanceFilter.builder().service(idFilter).build();
    } else if (NameService.environment.equals(key)) {
      return QLInstanceFilter.builder().environment(idFilter).build();
    } else if (NameService.cloudProvider.equals(key)) {
      return QLInstanceFilter.builder().cloudProvider(idFilter).build();
    }
    throw new WingsException("Unsupported field " + key + " while generating filter");
  }
}
