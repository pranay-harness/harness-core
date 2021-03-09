package io.harness.ccm.views.graphql.anomalydetection;

import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeTruncGroupBy;
import io.harness.ccm.views.graphql.ViewsQueryBuilder;
import io.harness.exception.InvalidRequestException;

import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ADViewsQueryBuilder extends ViewsQueryBuilder {
  DbFieldMapper dbFieldMapper = new DbFieldMapper();

  public SelectQuery getQuery(List<ViewRule> rules, List<QLCEViewFilter> filters, List<QLCEViewTimeFilter> timeFilters,
      List<QLCEViewGroupBy> groupByList, List<QLCEViewAggregation> aggregations,
      List<QLCEViewSortCriteria> sortCriteriaList) {
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addAllTableColumns(AnomalyEntity.AnomaliesDataTableSchema.table);

    List<QLCEViewFieldInput> groupByEntity = getGroupByEntity(groupByList);
    QLCEViewTimeTruncGroupBy groupByTime = getGroupByTime(groupByList);

    // convert group by to filters
    for (QLCEViewGroupBy groupBy : groupByList) {
      filters.add(convertGroupByToFilter(groupBy));
    }

    List<ViewField> customFields = collectCustomFieldList(rules, filters, groupByEntity);
    modifyQueryWithInstanceTypeFilter(rules, filters, groupByEntity, customFields, selectQuery);

    if (!rules.isEmpty()) {
      selectQuery.addCondition(getConsolidatedRuleCondition(rules));
    }

    if (!filters.isEmpty()) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    if (!timeFilters.isEmpty()) {
      decorateQueryWithTimeFilters(selectQuery, timeFilters);
    }

    if (groupByTime != null) {
      decorateQueryWithGroupByTime(selectQuery, groupByTime);
    }

    if (!aggregations.isEmpty()) {
      decorateQueryWithAggregations(selectQuery, aggregations);
    }

    if (!sortCriteriaList.isEmpty()) {
      decorateQueryWithSortCriteria(selectQuery, sortCriteriaList);
    }

    log.info("Query for view {}", selectQuery.toString());
    return selectQuery;
  }

  protected QLCEViewFilter convertGroupByToFilter(QLCEViewGroupBy groupBy) {
    return QLCEViewFilter.builder().field(groupBy.getEntityGroupBy()).operator(QLCEViewFilterOperator.NOT_NULL).build();
  }

  @Override
  protected Object getSQLObjectFromField(QLCEViewFieldInput field) {
    switch (field.getIdentifier()) {
      case AWS:
      case GCP:
      case CLUSTER:
      case COMMON:
        return new CustomSql(dbFieldMapper.get(field.getFieldId()));
      case LABEL:
      default:
        throw new InvalidRequestException("Invalid View Field Identifier " + field.getIdentifier());
    }
  }
}
