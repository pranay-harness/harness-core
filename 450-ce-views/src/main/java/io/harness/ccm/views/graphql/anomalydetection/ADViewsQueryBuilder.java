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
import io.harness.ccm.views.graphql.QLCEViewTimeGroupType;
import io.harness.ccm.views.graphql.QLCEViewTimeTruncGroupBy;
import io.harness.ccm.views.graphql.ViewsQueryBuilder;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ADViewsQueryBuilder extends ViewsQueryBuilder {
  DbFieldMapper dbFieldMapper = new DbFieldMapper();

  public SelectQuery getQuery(List<ViewRule> rules, List<QLCEViewFilter> filters, List<QLCEViewTimeFilter> timeFilters,
      List<QLCEViewGroupBy> groupByList) {
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addAllTableColumns(AnomalyEntity.AnomaliesDataTableSchema.table);

    for (QLCEViewGroupBy groupBy : groupByList) {
      if (convertGroupByToFilter(groupBy) != null) {
        if (groupBy.getEntityGroupBy() != null) {
          // convert entity group by to filters
          filters.add(convertGroupByToFilter(groupBy));
        } else if (groupBy.getTimeTruncGroupBy().getResolution() != QLCEViewTimeGroupType.DAY) {
          // only daily anomalies are supported for now !!
          throw new InvalidArgumentsException("GroupbyTime is missing returning empty list");
        }
      }
    }

    //    List<ViewField> customFields = collectCustomFieldList(rules, filters, groupByEntity);
    //    modifyQueryWithInstanceTypeFilter(rules, filters, groupByEntity, customFields, selectQuery);

    if (!rules.isEmpty()) {
      selectQuery.addCondition(getConsolidatedRuleCondition(rules));
    }

    if (!filters.isEmpty()) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    if (!timeFilters.isEmpty()) {
      decorateQueryWithTimeFilters(selectQuery, timeFilters);
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

  @Override
  protected Condition getConsolidatedRuleCondition(List<ViewRule> rules) {
    List<Condition> conditionList = new ArrayList<>();
    for (ViewRule rule : rules) {
      try {
        conditionList.add(getPerRuleCondition(rule));
      } catch (InvalidArgumentsException e) {
        log.warn("Skipping this view rules since it contains unsupported things ");
      }
    }
    return getSqlOrCondition(conditionList);
  }
}
