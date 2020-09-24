package io.harness.ccm.views.graphql;

import com.google.inject.Inject;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import io.harness.ccm.views.dao.ViewCustomFieldDao;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class ViewsQueryBuilder {
  @Inject ViewCustomFieldDao viewCustomFieldDao;
  private static final String leftJoinLabels = " LEFT JOIN UNNEST(labels) as labels";
  private static final String labelsFilter = "CONCAT(labels.key, ':', labels.value)";

  public SelectQuery getQuery(List<QLCEViewRule> rules, List<QLCEViewFilter> filters,
      List<QLCEViewTimeFilter> timeFilters, List<QLCEViewGroupBy> groupByList, List<QLCEViewAggregation> aggregations,
      List<QLCEViewSortCriteria> sortCriteriaList, List<ViewField> customFields) {
    SelectQuery selectQuery = new SelectQuery();
    boolean isLabelsPresent = false;
    List<QLCEViewFieldInput> groupByEntity = getGroupByEntity(groupByList);
    QLCEViewTimeTruncGroupBy groupByTime = getGroupByTime(groupByList);

    if (!customFields.isEmpty()) {
      List<String> labelsKeysListAcrossCustomFields = new ArrayList<>();
      for (ViewField field : customFields) {
        ViewCustomField customField = viewCustomFieldDao.getById(field.getFieldId());
        final List<String> labelsKeysList = customField.getViewFields()
                                                .stream()
                                                .filter(f -> f.getIdentifier() == ViewFieldIdentifier.LABEL)
                                                .map(ViewField::getFieldName)
                                                .collect(Collectors.toList());
        labelsKeysListAcrossCustomFields.addAll(labelsKeysList);
        if (!labelsKeysList.isEmpty()) {
          isLabelsPresent = true;
        }
        selectQuery.addAliasedColumn(customField.getSqlFormula(), customField.getName());
      }
      if (!labelsKeysListAcrossCustomFields.isEmpty()) {
        String[] labelsKeysListAcrossCustomFieldsStringArray =
            labelsKeysListAcrossCustomFields.toArray(new String[labelsKeysListAcrossCustomFields.size()]);

        filters.add(QLCEViewFilter.builder()
                        .field(QLCEViewFieldInput.builder()
                                   .fieldId(ViewsMetaDataFields.LABEL_KEY.getFieldName())
                                   .identifier(ViewFieldIdentifier.LABEL)
                                   .build())
                        .operator(QLCEViewFilterOperator.IN)
                        .values(labelsKeysListAcrossCustomFieldsStringArray)
                        .build());
      }
    }

    isLabelsPresent = isLabelsPresent || evaluateLabelsPresent(rules, filters, groupByEntity);

    if (isLabelsPresent) {
      selectQuery.addCustomJoin(leftJoinLabels);
      selectQuery.addCustomGroupings(ViewsMetaDataFields.LABEL_KEY.getAlias());
      selectQuery.addCustomGroupings(ViewsMetaDataFields.LABEL_VALUE.getAlias());
      selectQuery.addCustomColumns(
          ViewsMetaDataFields.LABEL_KEY.getFieldName(), ViewsMetaDataFields.LABEL_KEY.getAlias());
      selectQuery.addCustomColumns(
          ViewsMetaDataFields.LABEL_VALUE.getFieldName(), ViewsMetaDataFields.LABEL_VALUE.getAlias());
    }

    if (!rules.isEmpty()) {
      selectQuery.addCondition(getConsolidatedRuleCondition(rules));
    }

    if (!filters.isEmpty()) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    if (!timeFilters.isEmpty()) {
      decorateQueryWithTimeFilters(selectQuery, timeFilters);
    }

    if (!groupByEntity.isEmpty()) {
      for (QLCEViewFieldInput groupBy : groupByEntity) {
        if (groupBy.getIdentifier() != ViewFieldIdentifier.LABEL) {
          Object sqlObjectFromField = getSQLObjectFromField(groupBy);
          selectQuery.addCustomColumns(sqlObjectFromField);
          selectQuery.addCustomGroupings(sqlObjectFromField);
        }
      }
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

    return null;
  }

  private boolean evaluateLabelsPresent(
      List<QLCEViewRule> rules, List<QLCEViewFilter> filters, List<QLCEViewFieldInput> groupByEntity) {
    boolean labelFilterPresent =
        filters.stream().anyMatch(f -> f.getField().getIdentifier() == ViewFieldIdentifier.LABEL);
    boolean labelConditionPresent = false;

    for (QLCEViewRule rule : rules) {
      labelConditionPresent = labelConditionPresent
          || rule.getConditions().stream().anyMatch(c -> c.getField().getIdentifier() == ViewFieldIdentifier.LABEL);
    }

    boolean labelGroupByPresent = groupByEntity.stream().anyMatch(g -> g.getIdentifier() == ViewFieldIdentifier.LABEL);

    return labelFilterPresent || labelConditionPresent || labelGroupByPresent;
  }

  private void decorateQueryWithSortCriteria(SelectQuery selectQuery, List<QLCEViewSortCriteria> sortCriteriaList) {
    for (QLCEViewSortCriteria sortCriteria : sortCriteriaList) {
      addOrderBy(selectQuery, sortCriteria);
    }
  }

  private void addOrderBy(SelectQuery selectQuery, QLCEViewSortCriteria sortCriteria) {
    Object sortKey = getSQLObjectFromField(sortCriteria.getSortType());
    OrderObject.Dir dir =
        sortCriteria.getSortOrder() == QLCESortOrder.ASCENDING ? OrderObject.Dir.ASCENDING : OrderObject.Dir.DESCENDING;
    selectQuery.addCustomOrdering(sortKey, dir);
  }

  private void decorateQueryWithAggregations(SelectQuery selectQuery, List<QLCEViewAggregation> aggregations) {
    for (QLCEViewAggregation aggregation : aggregations) {
      decorateQueryWithAggregation(selectQuery, aggregation);
    }
  }

  private void decorateQueryWithAggregation(SelectQuery selectQuery, QLCEViewAggregation aggregation) {
    FunctionCall functionCall = getFunctionCallType(aggregation.getOperationType());
    if (aggregation.getColumnName().equals(ViewsMetaDataFields.COST.getFieldName())) {
      selectQuery.addCustomColumns(Converter.toCustomColumnSqlObject(
          functionCall.addCustomParams(new CustomSql(ViewsMetaDataFields.COST.getFieldName())),
          ViewsMetaDataFields.COST.getFieldName()));
    }
  }

  private FunctionCall getFunctionCallType(QLCEViewAggregateOperation operationType) {
    switch (operationType) {
      case SUM:
        return FunctionCall.sum();
      default:
        return null;
    }
  }

  private void decorateQueryWithGroupByTime(SelectQuery selectQuery, QLCEViewTimeTruncGroupBy groupByTime) {
    selectQuery.addCustomColumns(Converter.toCustomColumnSqlObject(
        new TimeTruncatedExpression(
            new CustomSql(ViewsMetaDataFields.START_TIME.getFieldName()), groupByTime.getResolution()),
        ViewsMetaDataFields.TIME_GRANULARITY.getFieldName()));

    selectQuery.addCustomGroupings(ViewsMetaDataFields.TIME_GRANULARITY.getFieldName());
    selectQuery.addCustomOrdering(ViewsMetaDataFields.TIME_GRANULARITY.getFieldName(), OrderObject.Dir.ASCENDING);
  }

  protected List<QLCEViewFieldInput> getGroupByEntity(List<QLCEViewGroupBy> groupBy) {
    return groupBy != null ? groupBy.stream()
                                 .filter(g -> g.getEntityGroupBy() != null)
                                 .map(QLCEViewGroupBy::getEntityGroupBy)
                                 .collect(Collectors.toList())
                           : Collections.emptyList();
  }
  protected QLCEViewTimeTruncGroupBy getGroupByTime(List<QLCEViewGroupBy> groupBy) {
    if (groupBy != null) {
      Optional<QLCEViewTimeTruncGroupBy> first = groupBy.stream()
                                                     .filter(g -> g.getTimeTruncGroupBy() != null)
                                                     .map(QLCEViewGroupBy::getTimeTruncGroupBy)
                                                     .findFirst();
      return first.orElse(QLCEViewTimeTruncGroupBy.builder().resolution(QLCEViewTimeGroupType.DAY).build());
    }
    return null;
  }

  private Condition getConsolidatedRuleCondition(List<QLCEViewRule> rules) {
    List<Condition> conditionList = new ArrayList<>();
    for (QLCEViewRule rule : rules) {
      conditionList.add(getPerRuleCondition(rule));
    }
    return getSqlOrCondition(conditionList);
  }

  private Condition getPerRuleCondition(QLCEViewRule rule) {
    List<Condition> conditionList = new ArrayList<>();
    for (QLCEViewFilter filter : rule.getConditions()) {
      conditionList.add(getCondition(filter));
    }
    return getSqlAndCondition(conditionList);
  }

  private static Condition getSqlAndCondition(List<Condition> conditionList) {
    switch (conditionList.size()) {
      case 2:
        return ComboCondition.and(conditionList.get(0), conditionList.get(1));
      case 3:
        return ComboCondition.and(conditionList.get(0), conditionList.get(1), conditionList.get(2));
      case 4:
        return ComboCondition.and(
            conditionList.get(0), conditionList.get(1), conditionList.get(2), conditionList.get(3));
      case 5:
        return ComboCondition.and(conditionList.get(0), conditionList.get(1), conditionList.get(2),
            conditionList.get(3), conditionList.get(4));
      case 6:
        return ComboCondition.and(conditionList.get(0), conditionList.get(1), conditionList.get(2),
            conditionList.get(3), conditionList.get(4), conditionList.get(5));
      case 7:
        return ComboCondition.and(conditionList.get(0), conditionList.get(1), conditionList.get(2),
            conditionList.get(3), conditionList.get(4), conditionList.get(5), conditionList.get(6));
      case 8:
        return ComboCondition.and(conditionList.get(0), conditionList.get(1), conditionList.get(2),
            conditionList.get(3), conditionList.get(4), conditionList.get(5), conditionList.get(6),
            conditionList.get(7));
      case 9:
        return ComboCondition.and(conditionList.get(0), conditionList.get(1), conditionList.get(2),
            conditionList.get(3), conditionList.get(4), conditionList.get(5), conditionList.get(6),
            conditionList.get(7), conditionList.get(8));
      case 10:
        return ComboCondition.and(conditionList.get(0), conditionList.get(1), conditionList.get(2),
            conditionList.get(3), conditionList.get(4), conditionList.get(5), conditionList.get(6),
            conditionList.get(7), conditionList.get(8), conditionList.get(9));
      default:
        return conditionList.get(0);
    }
  }

  private static Condition getSqlOrCondition(List<Condition> conditionList) {
    switch (conditionList.size()) {
      case 2:
        return ComboCondition.or(conditionList.get(0), conditionList.get(1));
      case 3:
        return ComboCondition.or(conditionList.get(0), conditionList.get(1), conditionList.get(2));
      case 4:
        return ComboCondition.or(
            conditionList.get(0), conditionList.get(1), conditionList.get(2), conditionList.get(3));
      case 5:
        return ComboCondition.or(conditionList.get(0), conditionList.get(1), conditionList.get(2), conditionList.get(3),
            conditionList.get(4));
      case 6:
        return ComboCondition.or(conditionList.get(0), conditionList.get(1), conditionList.get(2), conditionList.get(3),
            conditionList.get(4), conditionList.get(5));
      case 7:
        return ComboCondition.or(conditionList.get(0), conditionList.get(1), conditionList.get(2), conditionList.get(3),
            conditionList.get(4), conditionList.get(5), conditionList.get(6));
      case 8:
        return ComboCondition.or(conditionList.get(0), conditionList.get(1), conditionList.get(2), conditionList.get(3),
            conditionList.get(4), conditionList.get(5), conditionList.get(6), conditionList.get(7));
      case 9:
        return ComboCondition.or(conditionList.get(0), conditionList.get(1), conditionList.get(2), conditionList.get(3),
            conditionList.get(4), conditionList.get(5), conditionList.get(6), conditionList.get(7),
            conditionList.get(8));
      case 10:
        return ComboCondition.or(conditionList.get(0), conditionList.get(1), conditionList.get(2), conditionList.get(3),
            conditionList.get(4), conditionList.get(5), conditionList.get(6), conditionList.get(7),
            conditionList.get(8), conditionList.get(9));
      default:
        return conditionList.get(0);
    }
  }

  private void decorateQueryWithFilters(SelectQuery selectQuery, List<QLCEViewFilter> filters) {
    for (QLCEViewFilter filter : filters) {
      selectQuery.addCondition(getCondition(filter));
    }
  }

  private void decorateQueryWithTimeFilters(SelectQuery selectQuery, List<QLCEViewTimeFilter> timeFilters) {
    for (QLCEViewTimeFilter timeFilter : timeFilters) {
      selectQuery.addCondition(getCondition(timeFilter));
    }
  }

  private Condition getCondition(QLCEViewFilter filter) {
    Object conditionKey = getSQLObjectFromField(filter.getField());
    if (conditionKey.equals(ViewsMetaDataFields.LABEL_VALUE.getFieldName())) {
      conditionKey = new CustomSql(labelsFilter);
      String labelKey = filter.getField().getFieldName();
      String[] values = filter.getValues();
      for (int i = 0; i < values.length; i++) {
        values[i] = labelKey + ":" + values[i];
      }
    }
    QLCEViewFilterOperator operator = filter.getOperator();

    if (filter.getValues().length > 0 && operator == QLCEViewFilterOperator.EQUALS) {
      operator = QLCEViewFilterOperator.IN;
    }

    switch (operator) {
      case EQUALS:
        return BinaryCondition.equalTo(conditionKey, filter.getValues()[0]);
      case IN:
        return new InCondition(conditionKey, (Object[]) filter.getValues());
      default:
        throw new InvalidRequestException("Invalid View Filter operator: " + operator);
    }
  }

  private Condition getCondition(QLCEViewTimeFilter timeFilter) {
    Object conditionKey = getSQLObjectFromField(timeFilter.getField());
    QLCEViewTimeFilterOperator operator = timeFilter.getOperator();

    switch (operator) {
      case BEFORE:
        return BinaryCondition.lessThanOrEq(conditionKey, Instant.ofEpochMilli((Long) timeFilter.getValue()));
      case AFTER:
        return BinaryCondition.greaterThanOrEq(conditionKey, Instant.ofEpochMilli((Long) timeFilter.getValue()));
      default:
        throw new InvalidRequestException("Invalid View TimeFilter operator: " + operator);
    }
  }

  private Object getSQLObjectFromField(QLCEViewFieldInput field) {
    switch (field.getIdentifier()) {
      case AWS:
      case GCP:
      case CLUSTER:
      case COMMON:
      case LABEL:
        return new CustomSql(field.getFieldId());
      case CUSTOM:
        return new CustomSql(field.getFieldName());
      default:
        throw new InvalidRequestException("Invalid View Field Identifier " + field.getIdentifier());
    }
  }
}
