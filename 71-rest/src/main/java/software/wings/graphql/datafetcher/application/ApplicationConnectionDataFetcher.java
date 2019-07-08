package software.wings.graphql.datafetcher.application;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.graphql.schema.type.QLApplicationConnection;
import software.wings.graphql.schema.type.QLApplicationConnection.QLApplicationConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationFilter;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class ApplicationConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLApplicationFilter, QLNoOpSortCriteria, QLApplicationConnection> {
  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLApplicationConnection fetchConnection(List<QLApplicationFilter> serviceFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Application> query =
        populateFilters(wingsPersistence, null, Application.class).order(Sort.descending(ApplicationKeys.createdAt));

    QLApplicationConnectionBuilder connectionBuilder = QLApplicationConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, application -> {
      QLApplicationBuilder builder = QLApplication.builder();
      ApplicationController.populateApplication(application, builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }

  protected String getFilterFieldName(String filterType) {
    throw new WingsException("Unknown filter type" + filterType);
  }
}
