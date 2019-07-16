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
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ExecutionConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLExecutionFilter, QLNoOpSortCriteria, QLExecutionConnection> {
  @Inject private ExecutionController executionController;
  @Inject private AppService appService;
  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLExecutionConnection fetchConnection(List<QLExecutionFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    filters = addAppIdValidation(filters);
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
    // TODO to be implemented later
  }

  private List<QLExecutionFilter> addAppIdValidation(List<QLExecutionFilter> filters) {
    List<QLExecutionFilter> updatedFilters = filters != null ? new ArrayList<>(filters) : new ArrayList<>();
    boolean appIdFilterFound = false;
    if (EmptyPredicate.isNotEmpty(filters)) {
      for (QLExecutionFilter filter : filters) {
        if (filter.getType().equals(QLExecutionFilterType.Application)) {
          appIdFilterFound = true;
          break;
        }
      }
    }

    if (!appIdFilterFound) {
      List<String> appIds = appService.getAppIdsByAccountId(super.getAccountId());
      updatedFilters.add(
          QLExecutionFilter.builder()
              .type(QLExecutionFilterType.Application)
              .stringFilter(
                  QLStringFilter.builder().operator(QLStringOperator.IN).values(appIds.toArray(new String[0])).build())
              .build());
    }

    return updatedFilters;
  }

  protected String getFilterFieldName(String filterType) {
    QLExecutionFilterType type = QLExecutionFilterType.valueOf(filterType);
    switch (type) {
      case EndTime:
        return WorkflowExecutionKeys.endTs;
      case StartTime:
        return WorkflowExecutionKeys.startTs;
      case Service:
        return WorkflowExecutionKeys.serviceIds;
      case Trigger:
        return WorkflowExecutionKeys.deploymentTriggerId;
      case TriggeredBy:
        return WorkflowExecutionKeys.triggeredBy;
      case CloudProvider:
        return WorkflowExecutionKeys.cloudProviderIds;
      case Environment:
        return WorkflowExecutionKeys.envIds;
      case Pipeline:
      case Workflow:
        return WorkflowExecutionKeys.workflowId;
      case Status:
        return WorkflowExecutionKeys.status;
      case Application:
        return WorkflowExecutionKeys.appId;
      case CreatedAt:
        return WorkflowExecutionKeys.createdAt;
      // TODO
      case Duration:
      default:
        throw new WingsException("Unsupported type " + type);
    }
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

    // TODO Rushabh
    //    if (NameService.application.equals(key)) {
    //      return QLExecutionFilter.builder().application(idFilter).build();
    //    } else if (NameService.service.equals(key)) {
    //      return QLExecutionFilter.builder().service(idFilter).build();
    //    } else if (NameService.environment.equals(key)) {
    //      return QLExecutionFilter.builder().environment(idFilter).build();
    //    } else if (NameService.cloudProvider.equals(key)) {
    //      return QLExecutionFilter.builder().cloudProvider(idFilter).build();
    //    }
    throw new WingsException("Unsupported field " + key + " while generating filter");
  }
}
