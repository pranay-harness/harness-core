package software.wings.graphql.datafetcher.anomaly;

import io.harness.ccm.anomaly.graphql.AnomaliesFilter;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;
import io.harness.ccm.billing.graphql.CloudBillingIdFilter;
import io.harness.ccm.billing.graphql.CloudEntityGroupBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyInput;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter.QLBillingDataFilterBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilterType;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.UpdateQuery;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnomalyDataQueryBuilder {
  private void addAccountFilter(SelectQuery selectQuery, String accountId) {
    selectQuery.addCondition(BinaryCondition.equalTo(AnomaliesDataTableSchema.accountId, accountId));
  }

  public String formAnomalyFetchQuery(String accountId, QLAnomalyInput input) {
    SelectQuery query = new SelectQuery();
    query.addAllTableColumns(AnomaliesDataTableSchema.table);
    addAccountFilter(query, accountId);
    query.addCondition(BinaryCondition.equalTo(AnomaliesDataTableSchema.id, input.getAnomalyId()));

    return query.toString();
  }

  public String formAnomalyUpdateQuery(String accountId, QLAnomalyInput input) {
    if (input.getAnomalyId() == null) {
      log.error(
          "Mutation request to update anomaly has anomalyid equal to null, cannot process mutation request id : {}",
          input.getClientMutationId());
      throw new InvalidArgumentsException("Mutation request cannot be processed since anomalyid is null");
    }

    UpdateQuery query = new UpdateQuery(AnomaliesDataTableSchema.table);
    query.addCondition(BinaryCondition.equalTo(AnomaliesDataTableSchema.accountId, accountId));
    query.addCondition(BinaryCondition.equalTo(AnomaliesDataTableSchema.id, input.getAnomalyId()));

    if (input.getComment() != null) {
      query.addSetClause(AnomaliesDataTableSchema.note, input.getComment());
    }

    if (input.getUserFeedback() != null) {
      query.addCustomSetClause(AnomaliesDataTableSchema.feedBack, input.getUserFeedback().toString());
    }

    return query.validate().toString();
  }

  public String overviewQuery(String accountId, List<QLBillingDataFilter> filters) {
    filters = new ArrayList<QLBillingDataFilter>(filters);

    SelectQuery query = new SelectQuery();
    addAccountFilter(query, accountId);
    query.addAllTableColumns(AnomaliesDataTableSchema.table);
    query.addAliasedColumn(new CustomSql(AnomaliesDataTableSchema.actualCost.getColumnNameSQL() + " - "
                               + AnomaliesDataTableSchema.expectedCost.getColumnNameSQL()),
        "difference");
    decorateK8SQueryWithFilters(query, filters);
    query.addOrdering(AnomaliesDataTableSchema.anomalyTime, OrderObject.Dir.ASCENDING);
    query.addCustomOrdering(new CustomSql("difference"), OrderObject.Dir.DESCENDING);
    return query.toString();
  }

  public String formK8SQuery(String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy) {
    filters = new ArrayList<QLBillingDataFilter>(filters);

    SelectQuery query = new SelectQuery();
    addAccountFilter(query, accountId);
    query.addAllTableColumns(AnomaliesDataTableSchema.table);
    query.addAliasedColumn(new CustomSql(AnomaliesDataTableSchema.actualCost.getColumnNameSQL() + " - "
                               + AnomaliesDataTableSchema.expectedCost.getColumnNameSQL()),
        "difference");

    convertK8SGroupByAndAddToFilter(groupBy, filters);
    decorateK8SQueryWithFilters(query, filters);
    query.addOrdering(AnomaliesDataTableSchema.anomalyTime, OrderObject.Dir.ASCENDING);
    query.addCustomOrdering(new CustomSql("difference"), OrderObject.Dir.DESCENDING);
    return query.toString();
  }

  protected List<QLCCMEntityGroupBy> getK8SGroupByEntity(List<QLCCMGroupBy> groupBy) {
    return groupBy != null ? groupBy.stream()
                                 .filter(g -> g.getEntityGroupBy() != null)
                                 .map(QLCCMGroupBy::getEntityGroupBy)
                                 .collect(Collectors.toList())
                           : Collections.emptyList();
  }

  private void convertK8SGroupByAndAddToFilter(List<QLCCMGroupBy> groupBy, List<QLBillingDataFilter> filters) {
    List<QLCCMEntityGroupBy> entityGroupBy = getK8SGroupByEntity(groupBy);
    for (QLCCMEntityGroupBy singleGroupBy : entityGroupBy) {
      filters.add(convertGroupByToFilter(singleGroupBy));
    }
  }

  private QLBillingDataFilter convertGroupByToFilter(QLCCMEntityGroupBy groupBy) {
    QLBillingDataFilterBuilder filter = QLBillingDataFilter.builder();
    String[] values = new String[] {""};
    switch (groupBy) {
      case Cluster:
        filter.cluster(QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(values).build());
        break;
      case Namespace:
        filter.namespace(QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(values).build());
        break;
      case WorkloadName:
        filter.workloadName(QLIdFilter.builder().operator(QLIdOperator.NOT_NULL).values(values).build());
        break;
      case WorkloadType:
      case CloudServiceName:
      case Application:
      case Service:
      case StartTime:
      case Region:
      case Environment:
      case TaskId:
      case LaunchType:
      case ClusterType:
      case ClusterName:
      case InstanceType:
      case InstanceName:
      case CloudProvider:
      case Node:
      case Pod:
      default:
        log.error("Groupby clause not supported in AnomalyDataQueryBuilder");
        throw new InvalidArgumentsException("Entity-Groupby clause not supported");
    }
    return filter.build();
  }

  private void decorateK8SQueryWithFilters(SelectQuery selectQuery, List<QLBillingDataFilter> filters) {
    for (QLBillingDataFilter filter : filters) {
      Set<QLBillingDataFilterType> filterTypes = QLBillingDataFilter.getFilterTypes(filter);
      for (QLBillingDataFilterType type : filterTypes) {
        if (type.getMetaDataFields().getFilterKind() == QLFilterKind.SIMPLE) {
          decorateSimpleFilter(selectQuery, filter, type);
        } else {
          log.error("Failed to apply K8S filter :[{}]", filter);
        }
      }
    }
  }

  private void decorateSimpleFilter(SelectQuery selectQuery, QLBillingDataFilter filter, QLBillingDataFilterType type) {
    Filter f = QLBillingDataFilter.getFilter(type, filter);
    if (checkFilter(f)) {
      if (isIdFilter(f)) {
        addSimpleIdOperator(selectQuery, f, type);
      } else if (isTimeFilter(f)) {
        addSimpleTimeFilter(selectQuery, f, type);
      }
    } else {
      log.info("Not adding K8S filter since it is not valid " + f);
    }
  }

  private void addSimpleTimeFilter(SelectQuery selectQuery, Filter filter, QLBillingDataFilterType type) {
    DbColumn key = getFilterKey(type);
    QLTimeFilter timeFilter = (QLTimeFilter) filter;
    switch (timeFilter.getOperator()) {
      case BEFORE:
        selectQuery.addCondition(BinaryCondition.lessThanOrEq(key, Instant.ofEpochMilli((Long) timeFilter.getValue())));
        break;
      case AFTER:
        selectQuery.addCondition(
            BinaryCondition.greaterThanOrEq(key, Instant.ofEpochMilli((Long) timeFilter.getValue())));
        break;
      default:
        throw new InvalidRequestException("Invalid TimeFilter operator: " + filter.getOperator());
    }
  }

  private void addSimpleIdOperator(SelectQuery selectQuery, Filter filter, QLBillingDataFilterType type) {
    DbColumn key = getFilterKey(type);
    QLIdOperator operator = (QLIdOperator) filter.getOperator();
    QLIdOperator finalOperator = operator;
    if (filter.getValues().length > 0) {
      if (operator == QLIdOperator.EQUALS) {
        finalOperator = QLIdOperator.IN;
        log.info("Changing simpleStringOperator from [{}] to [{}]", operator, finalOperator);
      } else {
        finalOperator = operator;
      }
    }
    switch (finalOperator) {
      case EQUALS:
        selectQuery.addCondition(BinaryCondition.equalTo(key, filter.getValues()[0]));
        break;
      case IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getValues()));
        break;
      case NOT_NULL:
        selectQuery.addCondition(UnaryCondition.isNotNull(key));
        break;
      case NOT_IN:
        InCondition inCondition = new InCondition(key, (Object[]) filter.getValues());
        inCondition.setNegate(true);
        selectQuery.addCondition(inCondition);
        break;
      case LIKE:
        selectQuery.addCondition(BinaryCondition.like(key, "%" + filter.getValues()[0] + "%"));
        break;
      default:
        throw new InvalidRequestException("String simple operator not supported" + operator);
    }
  }

  private boolean isIdFilter(Filter f) {
    return f instanceof QLIdFilter;
  }

  private boolean isTimeFilter(Filter f) {
    return f instanceof QLTimeFilter;
  }

  private boolean checkFilter(Filter f) {
    return f.getOperator() != null && EmptyPredicate.isNotEmpty(f.getValues());
  }

  private DbColumn getFilterKey(QLBillingDataFilterType type) {
    switch (type) {
      case EndTime:
      case StartTime:
        return AnomaliesDataTableSchema.anomalyTime;
      case Cluster:
        return AnomaliesDataTableSchema.clusterId;
      case WorkloadName:
        return AnomaliesDataTableSchema.workloadType;
      case Namespace:
        return AnomaliesDataTableSchema.namespace;
      case Application:
      case Service:
      case Environment:
      case CloudServiceName:
      case LaunchType:
      case TaskId:
      case InstanceType:
      case InstanceName:
      case CloudProvider:
      case NodeInstanceId:
      case PodInstanceId:
      case ParentInstanceId:
      case LabelSearch:
      case TagSearch:
      case Tag:
      case Label:
      case EnvironmentType:
      case AlertTime:
        break;
      default:
        throw new InvalidArgumentsException("Filter type not supported " + type);
    }
    return null;
  }

  //----------------- Cloud -------------

  public String formCloudQuery(String accountId, List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy) {
    filters = new ArrayList<CloudBillingFilter>(filters);
    SelectQuery query = new SelectQuery();
    addAccountFilter(query, accountId);
    query.addAllTableColumns(AnomaliesDataTableSchema.table);
    query.addAliasedColumn(new CustomSql(AnomaliesDataTableSchema.actualCost.getColumnNameSQL() + " - "
                               + AnomaliesDataTableSchema.expectedCost.getColumnNameSQL()),
        "difference");
    convertCloudGroupByAndAddToFilter(filters, groupBy);
    decorateCloudQueryWithFilters(query, filters);
    query.addOrdering(AnomaliesDataTableSchema.anomalyTime, OrderObject.Dir.ASCENDING);
    query.addCustomOrdering(new CustomSql("difference"), OrderObject.Dir.DESCENDING);
    return query.toString();
  }

  private void convertCloudGroupByAndAddToFilter(List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy) {
    List<CloudEntityGroupBy> entityGroupBy = getCloudGroupByEntity(groupBy);
    for (CloudEntityGroupBy singleGroupBy : entityGroupBy) {
      filters.add(convertGroupByToFilter(singleGroupBy));
    }
  }
  protected List<CloudEntityGroupBy> getCloudGroupByEntity(List<CloudBillingGroupBy> groupBy) {
    return groupBy != null ? groupBy.stream()
                                 .filter(g -> g.getEntityGroupBy() != null)
                                 .map(CloudBillingGroupBy::getEntityGroupBy)
                                 .collect(Collectors.toList())
                           : Collections.emptyList();
  }

  private CloudBillingFilter convertGroupByToFilter(CloudEntityGroupBy groupBy) {
    CloudBillingFilter filter = new CloudBillingFilter();
    String[] values = new String[] {""};
    // TODO: support all relavant groupby
    switch (groupBy) {
      // --- GCP ---
      case projectId:
        filter.setProjectId(CloudBillingIdFilter.builder()
                                .operator(QLIdOperator.NOT_NULL)
                                .variable(CloudBillingFilter.BILLING_GCP_PROJECT)
                                .values(values)
                                .build());
        break;
      case skuId:
        filter.setSku(CloudBillingIdFilter.builder()
                          .operator(QLIdOperator.NOT_NULL)
                          .variable(CloudBillingFilter.BILLING_GCP_SKU)
                          .values(values)
                          .build());
        break;
      case product:
        filter.setProduct(CloudBillingIdFilter.builder()
                              .operator(QLIdOperator.NOT_NULL)
                              .variable(CloudBillingFilter.BILLING_GCP_PRODUCT)
                              .values(values)
                              .build());
        break;
      case sku:
        break;
      // -- aws --
      case awsLinkedAccount:
        filter.setAwsLinkedAccount(CloudBillingIdFilter.builder()
                                       .operator(QLIdOperator.NOT_NULL)
                                       .variable(CloudBillingFilter.BILLING_AWS_LINKED_ACCOUNT)
                                       .values(values)
                                       .build());
        break;
      case awsService:
        filter.setAwsService(CloudBillingIdFilter.builder()
                                 .operator(QLIdOperator.NOT_NULL)
                                 .variable(CloudBillingFilter.BILLING_AWS_SERVICE)
                                 .values(values)
                                 .build());
        break;
      case billingAccountId:
      case awsUsageType:
      case awsInstanceType:
      case cloudProvider:
      case labelsKey:
      case projectNumber:
      case labelsValue:
      case tagsKey:
      case tagsValue:
      default:
        log.error("Cloud Groupby clause not supported in AnomalyDataQueryBuilder");
        throw new InvalidArgumentsException("Entity-Groupby clause not supported");
    }
    return filter;
  }

  private void decorateCloudQueryWithFilters(SelectQuery selectQuery, List<CloudBillingFilter> filters) {
    AnomaliesFilter cloudFilter;
    for (CloudBillingFilter filter : filters) {
      try {
        cloudFilter = AnomaliesFilter.convertFromCloudBillingFilter(filter);
        selectQuery.addCondition(cloudFilter.toCondition());
      } catch (Exception e) {
        log.info("Not adding filter since it is not valid ");
      }
    }
  }
}