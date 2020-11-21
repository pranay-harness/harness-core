package software.wings.graphql.datafetcher.application;

import static software.wings.graphql.utils.nameservice.NameService.application;

import io.harness.exception.InvalidRequestException;

import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.graphql.schema.type.QLApplication.QLApplicationBuilder;
import software.wings.graphql.schema.type.QLApplicationConnection;
import software.wings.graphql.schema.type.QLApplicationConnection.QLApplicationConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.application.QLApplicationFilter;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

@Slf4j
public class ApplicationConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLApplicationFilter, QLNoOpSortCriteria, QLApplicationConnection> {
  @Inject ApplicationQueryHelper applicationQueryHelper;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLApplicationConnection fetchConnection(List<QLApplicationFilter> appFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Application> query = populateFilters(wingsPersistence, appFilters, Application.class, true);
    query.order(Sort.descending(ApplicationKeys.createdAt));

    QLApplicationConnectionBuilder connectionBuilder = QLApplicationConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, application -> {
      QLApplicationBuilder builder = QLApplication.builder();
      ApplicationController.populateQLApplication(application, builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLApplicationFilter> filters, Query query) {
    applicationQueryHelper.setQuery(filters, query, getAccountId());
  }

  @Override
  protected QLApplicationFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    QLIdFilter idFilter = QLIdFilter.builder()
                              .operator(QLIdOperator.EQUALS)
                              .values(new String[] {(String) utils.getFieldValue(environment.getSource(), value)})
                              .build();
    if (application.equals(key)) {
      return QLApplicationFilter.builder().application(idFilter).build();
    }
    throw new InvalidRequestException("Unsupported field " + key + " while generating filter");
  }
}
