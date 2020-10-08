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
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgLimitClause;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgOffsetClause;
import io.harness.ccm.views.dao.ViewCustomFieldDao;
import io.harness.ccm.views.entities.ViewCondition;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewIdOperator;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.entities.ViewTimeGranularity;
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
  private static final String distinct = " DISTINCT(%s)";
  private static final String aliasStartTimeMaxMin = "%s_%s";
  private static final String labelsFilter = "CONCAT(labels.key, ':', labels.value)";

  public SelectQuery getQuery(List<ViewRule> rules, List<QLCEViewFilter> filters, List<QLCEViewTimeFilter> timeFilters,
      List<QLCEViewGroupBy> groupByList, List<QLCEViewAggregation> aggregations,
      List<QLCEViewSortCriteria> sortCriteriaList, List<ViewField> customFields, String cloudProviderTableName) {
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addCustomFromTable(cloudProviderTableName);
    boolean isLabelsPresent = false;
    List<QLCEViewFieldInput> groupByEntity = getGroupByEntity(groupByList);
    QLCEViewTimeTruncGroupBy groupByTime = getGroupByTime(groupByList);

    if (!customFields.isEmpty()) {
      isLabelsPresent = modifyQueryForCustomFields(filters, customFields, selectQuery);
    }

    isLabelsPresent = isLabelsPresent || evaluateLabelsPresent(rules, filters);
    boolean labelGroupByPresent = groupByEntity.stream().anyMatch(g -> g.getIdentifier() == ViewFieldIdentifier.LABEL);

    if (isLabelsPresent || labelGroupByPresent) {
      decorateQueryWithLabelsMetadata(selectQuery, isLabelsPresent, labelGroupByPresent);
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
          // Custom Fields are already added By Default in Select Objects
          if (groupBy.getIdentifier() != ViewFieldIdentifier.CUSTOM) {
            selectQuery.addCustomColumns(sqlObjectFromField);
            selectQuery.addCustomGroupings(sqlObjectFromField);
          } else {
            selectQuery.addCustomGroupings(modifyStringToComplyRegex(groupBy.getFieldName()));
          }
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

    return selectQuery;
  }

  public ViewsQueryMetadata getFilterValuesQuery(
      List<QLCEViewFilter> filters, String cloudProviderTableName, Integer limit, Integer offset) {
    List<QLCEViewFieldInput> fields = new ArrayList<>();
    SelectQuery query = new SelectQuery();
    query.addCustomization(new PgLimitClause(limit));
    query.addCustomization(new PgOffsetClause(offset));
    query.addCustomFromTable(cloudProviderTableName);
    for (QLCEViewFilter filter : filters) {
      QLCEViewFieldInput viewFieldInput = filter.getField();
      switch (viewFieldInput.getIdentifier()) {
        case AWS:
        case GCP:
        case CLUSTER:
        case COMMON:
          query.addAliasedColumn(
              new CustomSql(String.format(distinct, viewFieldInput.getFieldId())), viewFieldInput.getFieldId());
          break;
        case LABEL:
          if (viewFieldInput.getFieldId().equals(ViewsMetaDataFields.LABEL_KEY.getFieldName())) {
            query.addCustomGroupings(ViewsMetaDataFields.LABEL_KEY.getAlias());
            query.addAliasedColumn(new CustomSql(String.format(distinct, viewFieldInput.getFieldId())),
                ViewsMetaDataFields.LABEL_KEY.getAlias());
          } else {
            query.addCustomGroupings(ViewsMetaDataFields.LABEL_VALUE.getAlias());
            query.addCondition(getCondition(getLabelKeyFilter(new String[] {viewFieldInput.getFieldName()})));
            query.addAliasedColumn(new CustomSql(String.format(distinct, viewFieldInput.getFieldId())),
                ViewsMetaDataFields.LABEL_VALUE.getAlias());
          }
          query.addCustomJoin(leftJoinLabels);
          break;
        case CUSTOM:
          ViewCustomField customField = viewCustomFieldDao.getById(viewFieldInput.getFieldId());
          List<String> labelsKeysList = getLabelsKeyList(customField);
          if (!labelsKeysList.isEmpty()) {
            decorateQueryWithLabelsMetadata(query, true, false);
            String[] labelsKeysListStringArray = labelsKeysList.toArray(new String[labelsKeysList.size()]);
            query.addCondition(getCondition(getLabelKeyFilter(labelsKeysListStringArray)));
          }
          query.addAliasedColumn(new CustomSql(String.format(distinct, customField.getSqlFormula())),
              modifyStringToComplyRegex(customField.getName()));
          break;
        default:
          throw new InvalidRequestException("Invalid View Field Identifier " + viewFieldInput.getIdentifier());
      }
      fields.add(filter.getField());
    }
    return ViewsQueryMetadata.builder().query(query).fields(fields).build();
  }

  private QLCEViewFilter getLabelKeyFilter(String[] values) {
    return QLCEViewFilter.builder()
        .field(QLCEViewFieldInput.builder()
                   .fieldId(ViewsMetaDataFields.LABEL_KEY.getFieldName())
                   .identifier(ViewFieldIdentifier.LABEL)
                   .build())
        .operator(QLCEViewFilterOperator.IN)
        .values(values)
        .build();
  }

  private void decorateQueryWithLabelsMetadata(
      SelectQuery selectQuery, boolean isLabelsPresent, boolean labelGroupByPresent) {
    if (isLabelsPresent || labelGroupByPresent) {
      selectQuery.addCustomJoin(leftJoinLabels);
    }
    if (labelGroupByPresent) {
      selectQuery.addCustomGroupings(ViewsMetaDataFields.LABEL_VALUE.getAlias());
      selectQuery.addCustomColumns(Converter.toCustomColumnSqlObject(
          ViewsMetaDataFields.LABEL_VALUE.getFieldName(), ViewsMetaDataFields.LABEL_VALUE.getAlias()));
    }
  }

  private boolean modifyQueryForCustomFields(
      List<QLCEViewFilter> filters, List<ViewField> customFields, SelectQuery selectQuery) {
    boolean isLabelsPresent = false;
    List<String> labelsKeysListAcrossCustomFields = new ArrayList<>();
    for (ViewField field : customFields) {
      ViewCustomField customField = viewCustomFieldDao.getById(field.getFieldId());
      final List<String> labelsKeysList = getLabelsKeyList(customField);
      labelsKeysListAcrossCustomFields.addAll(labelsKeysList);
      if (!labelsKeysList.isEmpty()) {
        isLabelsPresent = true;
      }
      selectQuery.addAliasedColumn(
          new CustomSql(customField.getSqlFormula()), modifyStringToComplyRegex(customField.getName()));
    }
    if (!labelsKeysListAcrossCustomFields.isEmpty()) {
      String[] labelsKeysListAcrossCustomFieldsStringArray =
          labelsKeysListAcrossCustomFields.toArray(new String[labelsKeysListAcrossCustomFields.size()]);
      filters.add(getLabelKeyFilter(labelsKeysListAcrossCustomFieldsStringArray));
    }
    return isLabelsPresent;
  }

  private List<String> getLabelsKeyList(ViewCustomField customField) {
    return customField.getViewFields()
        .stream()
        .filter(f -> f.getIdentifier() == ViewFieldIdentifier.LABEL)
        .map(ViewField::getFieldName)
        .collect(Collectors.toList());
  }

  private boolean evaluateLabelsPresent(List<ViewRule> rules, List<QLCEViewFilter> filters) {
    boolean labelFilterPresent =
        filters.stream().anyMatch(f -> f.getField().getIdentifier() == ViewFieldIdentifier.LABEL);
    boolean labelConditionPresent = false;

    for (ViewRule rule : rules) {
      labelConditionPresent = labelConditionPresent
          || rule.getViewConditions().stream().anyMatch(
                 c -> ((ViewIdCondition) c).getViewField().getIdentifier() == ViewFieldIdentifier.LABEL);
    }

    return labelFilterPresent || labelConditionPresent;
  }

  private void decorateQueryWithSortCriteria(SelectQuery selectQuery, List<QLCEViewSortCriteria> sortCriteriaList) {
    for (QLCEViewSortCriteria sortCriteria : sortCriteriaList) {
      addOrderBy(selectQuery, sortCriteria);
    }
  }

  private void addOrderBy(SelectQuery selectQuery, QLCEViewSortCriteria sortCriteria) {
    Object sortKey;
    switch (sortCriteria.getSortType()) {
      case COST:
        sortKey = ViewsMetaDataFields.COST.getAlias();
        break;
      case TIME:
        sortKey = ViewsMetaDataFields.START_TIME.getAlias();
        break;
      default:
        throw new InvalidRequestException("Sort type not supported");
    }
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
    if (aggregation.getColumnName().equals(ViewsMetaDataFields.START_TIME.getFieldName())) {
      selectQuery.addCustomColumns(Converter.toCustomColumnSqlObject(
          functionCall.addCustomParams(new CustomSql(ViewsMetaDataFields.START_TIME.getFieldName())),
          String.format(
              aliasStartTimeMaxMin, ViewsMetaDataFields.START_TIME.getFieldName(), aggregation.getOperationType())));
    }
  }

  private FunctionCall getFunctionCallType(QLCEViewAggregateOperation operationType) {
    switch (operationType) {
      case SUM:
        return FunctionCall.sum();
      case MAX:
        return FunctionCall.max();
      case MIN:
        return FunctionCall.min();
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
      return first.orElse(null);
    }
    return null;
  }

  private Condition getConsolidatedRuleCondition(List<ViewRule> rules) {
    List<Condition> conditionList = new ArrayList<>();
    for (ViewRule rule : rules) {
      conditionList.add(getPerRuleCondition(rule));
    }
    return getSqlOrCondition(conditionList);
  }

  private Condition getPerRuleCondition(ViewRule rule) {
    List<Condition> conditionList = new ArrayList<>();
    for (ViewCondition condition : rule.getViewConditions()) {
      conditionList.add(getCondition(mapConditionToFilter((ViewIdCondition) condition)));
    }
    return getSqlAndCondition(conditionList);
  }

  private QLCEViewFilter mapConditionToFilter(ViewIdCondition condition) {
    return QLCEViewFilter.builder()
        .field(getViewFieldInput(condition.getViewField()))
        .operator(mapViewIdOperatorToQLCEViewFilterOperator(condition.getViewOperator()))
        .values(getStringArray(condition.getValues()))
        .build();
  }

  private String[] getStringArray(List<String> values) {
    return values.toArray(new String[values.size()]);
  }

  private QLCEViewFilterOperator mapViewIdOperatorToQLCEViewFilterOperator(ViewIdOperator operator) {
    if (operator.equals(ViewIdOperator.IN)) {
      return QLCEViewFilterOperator.IN;
    } else if (operator.equals(ViewIdOperator.NOT_IN)) {
      return QLCEViewFilterOperator.NOT_IN;
    }
    return null;
  }

  public QLCEViewTimeGroupType mapViewTimeGranularityToQLCEViewTimeGroupType(ViewTimeGranularity timeGranularity) {
    if (timeGranularity.equals(ViewTimeGranularity.DAY)) {
      return QLCEViewTimeGroupType.DAY;
    } else if (timeGranularity.equals(ViewTimeGranularity.MONTH)) {
      return QLCEViewTimeGroupType.MONTH;
    }
    return null;
  }

  public QLCEViewFieldInput getViewFieldInput(ViewField field) {
    return QLCEViewFieldInput.builder()
        .fieldId(field.getFieldId())
        .fieldName(field.getFieldName())
        .identifier(field.getIdentifier())
        .build();
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
    if (conditionKey.toString().equals(ViewsMetaDataFields.LABEL_VALUE.getFieldName())) {
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
      case NOT_IN:
        return new InCondition(conditionKey, (Object[]) filter.getValues()).setNegate(true);
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
        return new CustomSql(viewCustomFieldDao.getById(field.getFieldId()).getSqlFormula());
      default:
        throw new InvalidRequestException("Invalid View Field Identifier " + field.getIdentifier());
    }
  }

  public String getAliasFromField(QLCEViewFieldInput field) {
    switch (field.getIdentifier()) {
      case AWS:
      case GCP:
      case CLUSTER:
      case COMMON:
        return field.getFieldId();
      case LABEL:
        if (field.getFieldId().equals(ViewsMetaDataFields.LABEL_KEY.getFieldName())) {
          return ViewsMetaDataFields.LABEL_KEY.getAlias();
        } else {
          return ViewsMetaDataFields.LABEL_VALUE.getAlias();
        }
      case CUSTOM:
        return modifyStringToComplyRegex(field.getFieldName());
      default:
        throw new InvalidRequestException("Invalid View Field Identifier " + field.getIdentifier());
    }
  }

  public String modifyStringToComplyRegex(String value) {
    return value.toLowerCase().replaceAll("[^a-z0-9]", "_");
  }
}
