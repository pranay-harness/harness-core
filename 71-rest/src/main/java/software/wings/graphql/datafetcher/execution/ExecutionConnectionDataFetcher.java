package software.wings.graphql.datafetcher.execution;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLExecution;
import software.wings.graphql.schema.type.QLExecutionConnection;
import software.wings.graphql.schema.type.QLExecutionConnection.QLExecutionConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ExecutionConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLExecutionFilter, QLNoOpSortCriteria, QLExecutionConnection> {
  @Inject private ExecutionController executionController;
  @Inject private ExecutionQueryHelper executionQueryHelper;
  @Inject private AppService appService;
  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLExecutionConnection fetchConnection(List<QLExecutionFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<WorkflowExecution> query = populateFilters(wingsPersistence, filters, WorkflowExecution.class)
                                         .order(Sort.descending(WorkflowExecutionKeys.createdAt));

    QLExecutionConnectionBuilder connectionBuilder = QLExecutionConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, execution -> {
      final QLExecution qlExecution = executionController.populateExecution(execution);
      connectionBuilder.node(qlExecution);
    }));

    return connectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLExecutionFilter> filters, Query query) {
    filters = addAppIdValidation(filters);
    executionQueryHelper.setQuery(filters, query);
  }

  private List<QLExecutionFilter> addAppIdValidation(List<QLExecutionFilter> filters) {
    List<QLExecutionFilter> updatedFilters = filters != null ? new ArrayList<>(filters) : new ArrayList<>();
    boolean appIdFilterFound = false;
    if (EmptyPredicate.isNotEmpty(filters)) {
      for (QLExecutionFilter filter : filters) {
        if (filter.getApplication() != null) {
          appIdFilterFound = true;
          break;
        }
      }
    }

    if (!appIdFilterFound) {
      List<String> appIds = appService.getAppIdsByAccountId(super.getAccountId());
      updatedFilters.add(
          QLExecutionFilter.builder()
              .application(QLIdFilter.builder().operator(QLIdOperator.IN).values(appIds.toArray(new String[0])).build())
              .build());
    }

    return updatedFilters;
  }

  @Override
  public String getAccountId() {
    return null;
  }

  @Override
  protected QLExecutionFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    QLIdFilter idFilter = QLIdFilter.builder()
                              .operator(QLIdOperator.EQUALS)
                              .values(new String[] {(String) utils.getFieldValue(environment.getSource(), value)})
                              .build();

    if (NameService.application.equals(key)) {
      return QLExecutionFilter.builder().application(idFilter).build();
    } else if (NameService.service.equals(key)) {
      return QLExecutionFilter.builder().service(idFilter).build();
    } else if (NameService.environment.equals(key)) {
      return QLExecutionFilter.builder().environment(idFilter).build();
    } else if (NameService.cloudProvider.equals(key)) {
      return QLExecutionFilter.builder().cloudProvider(idFilter).build();
    } else if (NameService.pipelineExecution.equals(key)) {
      return QLExecutionFilter.builder().pipelineExecution(idFilter).build();
    }
    throw new WingsException("Unsupported field " + key + " while generating filter");
  }
}
