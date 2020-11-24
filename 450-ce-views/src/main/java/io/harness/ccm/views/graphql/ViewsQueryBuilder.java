package io.harness.ccm.views.graphql;

import static io.harness.ccm.views.graphql.ViewsMetaDataFields.LABEL_KEY;

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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.CustomCondition;
import com.healthmarketscience.sqlbuilder.CustomSql;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgLimitClause;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgOffsetClause;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ViewsQueryBuilder {
  public static final String K8S_NODE = "K8S_NODE";
  public static final String K8S_POD = "K8S_POD";
  @Inject ViewCustomFieldDao viewCustomFieldDao;
  private static final String leftJoinLabels = " LEFT JOIN UNNEST(labels) as labels";
  private static final String leftJoinSelectiveLabels = " LEFT JOIN UNNEST(labels) as labels ON labels.key IN (%s)";
  private static final String distinct = " DISTINCT(%s)";
  private static final String aliasStartTimeMaxMin = "%s_%s";
  private static final String labelsFilter = "CONCAT(labels.key, ':', labels.value)";
  private static final String searchFilter = "REGEXP_CONTAINS( LOWER(%s), LOWER('%s') )";

  public SelectQuery getQuery(List<ViewRule> rules, List<QLCEViewFilter> filters, List<QLCEViewTimeFilter> timeFilters,
      List<QLCEViewGroupBy> groupByList, List<QLCEViewAggregation> aggregations,
      List<QLCEViewSortCriteria> sortCriteriaList, String cloudProviderTableName) {
    SelectQuery selectQuery = new SelectQuery();
    selectQuery.addCustomFromTable(cloudProviderTableName);
    boolean isLabelsPresent = false;
    List<QLCEViewFieldInput> groupByEntity = getGroupByEntity(groupByList);
    QLCEViewTimeTruncGroupBy groupByTime = getGroupByTime(groupByList);

    List<ViewField> customFields = collectCustomFieldList(rules, filters);
    List<String> labelKeysList = new ArrayList<>();
    modifyQueryWithInstanceTypeFilter(rules, filters, groupByEntity, customFields, selectQuery);

    if (!customFields.isEmpty()) {
      List<String> labelKeysListInCustomFields = modifyQueryForCustomFields(selectQuery, customFields);
      labelKeysList.addAll(modifyQueryForCustomFields(selectQuery, customFields));
      isLabelsPresent = !labelKeysListInCustomFields.isEmpty();
    }

    labelKeysList.addAll(collectLabelKeysList(rules, filters));

    isLabelsPresent = isLabelsPresent || evaluateLabelsPresent(rules, filters);
    boolean labelGroupByPresent = groupByEntity.stream().anyMatch(g -> g.getIdentifier() == ViewFieldIdentifier.LABEL);

    if (isLabelsPresent || labelGroupByPresent) {
      decorateQueryWithLabelsMetadata(selectQuery, isLabelsPresent, labelGroupByPresent, labelKeysList);
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
            selectQuery.addAliasedColumn(sqlObjectFromField, modifyStringToComplyRegex(groupBy.getFieldName()));
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

    log.info("Query for view {}", selectQuery.toString());
    return selectQuery;
  }

  private List<String> collectLabelKeysList(List<ViewRule> rules, List<QLCEViewFilter> filters) {
    List<ViewCondition> labelConditions = new ArrayList<>();
    List<QLCEViewFilter> labelFilters = filters.stream()
                                            .filter(f -> f.getField().getIdentifier() == ViewFieldIdentifier.LABEL)
                                            .collect(Collectors.toList());

    for (ViewRule rule : rules) {
      labelConditions.addAll(
          rule.getViewConditions()
              .stream()
              .filter(c -> ((ViewIdCondition) c).getViewField().getIdentifier() == ViewFieldIdentifier.LABEL)
              .collect(Collectors.toList()));
    }

    List<String> labelKeyList = new ArrayList<>();
    for (QLCEViewFilter labelFilter : labelFilters) {
      if (labelFilter.getField().getFieldId().equals(LABEL_KEY.getFieldName())) {
        labelKeyList.addAll(Arrays.asList(labelFilter.getValues()));
      } else {
        labelKeyList.add(labelFilter.getField().getFieldName());
      }
    }

    for (ViewCondition labelCondition : labelConditions) {
      if (((ViewIdCondition) labelCondition).getViewField().getFieldId().equals(LABEL_KEY.getFieldName())) {
        labelKeyList.addAll(((ViewIdCondition) labelCondition).getValues());
      } else {
        labelKeyList.add(((ViewIdCondition) labelCondition).getViewField().getFieldName());
      }
    }

    return labelKeyList;
  }

  private List<ViewField> collectCustomFieldList(List<ViewRule> rules, List<QLCEViewFilter> filters) {
    List<ViewField> customFieldLists = new ArrayList<>();
    for (ViewRule rule : rules) {
      for (ViewCondition condition : rule.getViewConditions()) {
        ViewIdCondition viewIdCondition = (ViewIdCondition) condition;
        ViewFieldIdentifier viewFieldIdentifier = viewIdCondition.getViewField().getIdentifier();
        if (viewFieldIdentifier.equals(ViewFieldIdentifier.CUSTOM)) {
          customFieldLists.add(((ViewIdCondition) condition).getViewField());
        }
      }
    }

    for (QLCEViewFilter filter : filters) {
      if (filter.getField().getIdentifier().equals(ViewFieldIdentifier.CUSTOM)) {
        customFieldLists.add(getViewField(filter.getField()));
      }
    }

    return customFieldLists;
  }

  public ViewField getViewField(QLCEViewFieldInput field) {
    return ViewField.builder()
        .fieldId(field.getFieldId())
        .fieldName(field.getFieldName())
        .identifier(field.getIdentifier())
        .identifierName(field.getIdentifier().getDisplayName())
        .build();
  }

  private void modifyQueryWithInstanceTypeFilter(List<ViewRule> rules, List<QLCEViewFilter> filters,
      List<QLCEViewFieldInput> groupByEntity, List<ViewField> customFields, SelectQuery selectQuery) {
    boolean isClusterConditionOrFilterPresent = false;
    boolean isPodFilterPresent = false;
    for (ViewRule rule : rules) {
      for (ViewCondition condition : rule.getViewConditions()) {
        ViewIdCondition viewIdCondition = (ViewIdCondition) condition;
        ViewFieldIdentifier viewFieldIdentifier = viewIdCondition.getViewField().getIdentifier();
        if (viewFieldIdentifier.equals(ViewFieldIdentifier.CLUSTER)) {
          isClusterConditionOrFilterPresent = true;
          String fieldId = viewIdCondition.getViewField().getFieldId();
          if (ImmutableSet.of("namespace", "workloadName").contains(fieldId)) {
            isPodFilterPresent = true;
          }
        }
      }
    }

    for (QLCEViewFilter filter : filters) {
      ViewFieldIdentifier viewFieldIdentifier = filter.getField().getIdentifier();
      if (viewFieldIdentifier.equals(ViewFieldIdentifier.CLUSTER)) {
        isClusterConditionOrFilterPresent = true;
        String fieldId = filter.getField().getFieldId();
        if (ImmutableSet.of("namespace", "workloadName").contains(fieldId)) {
          isPodFilterPresent = true;
        }
      }
    }

    for (QLCEViewFieldInput groupBy : groupByEntity) {
      if (groupBy.getIdentifier().equals(ViewFieldIdentifier.CLUSTER)) {
        isClusterConditionOrFilterPresent = true;
        if (ImmutableSet.of("namespace", "workloadName").contains(groupBy.getFieldId())) {
          isPodFilterPresent = true;
        }
      }
    }

    for (ViewField field : customFields) {
      ViewCustomField customField = viewCustomFieldDao.getById(field.getFieldId());
      List<ViewField> customFieldViewFields = customField.getViewFields();
      for (ViewField viewField : customFieldViewFields) {
        if (viewField.getIdentifier().equals(ViewFieldIdentifier.CLUSTER)) {
          isClusterConditionOrFilterPresent = true;
          if (ImmutableSet.of("namespace", "workloadName").contains(viewField.getFieldId())) {
            isPodFilterPresent = true;
          }
        }
      }
    }

    if (isClusterConditionOrFilterPresent) {
      String instanceType = K8S_NODE;
      if (isPodFilterPresent) {
        instanceType = K8S_POD;
      }

      List<Condition> conditionList = new ArrayList<>();
      conditionList.add(UnaryCondition.isNull(new CustomSql(ViewsMetaDataFields.INSTANCE_TYPE.getFieldName())));
      conditionList.add(
          BinaryCondition.equalTo(new CustomSql(ViewsMetaDataFields.INSTANCE_TYPE.getFieldName()), instanceType));
      selectQuery.addCondition(getSqlOrCondition(conditionList));
    }
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
      String searchString = "";
      if (filter.getValues().length != 0) {
        searchString = filter.getValues()[0];
      }
      switch (viewFieldInput.getIdentifier()) {
        case AWS:
        case GCP:
        case CLUSTER:
        case COMMON:
          query.addAliasedColumn(
              new CustomSql(String.format(distinct, viewFieldInput.getFieldId())), viewFieldInput.getFieldId());
          query.addCondition(
              new CustomCondition(String.format(searchFilter, viewFieldInput.getFieldId(), searchString)));
          break;
        case LABEL:
          if (viewFieldInput.getFieldId().equals(LABEL_KEY.getFieldName())) {
            query.addCustomGroupings(LABEL_KEY.getAlias());
            query.addAliasedColumn(
                new CustomSql(String.format(distinct, viewFieldInput.getFieldId())), LABEL_KEY.getAlias());
            query.addCondition(
                new CustomCondition(String.format(searchFilter, LABEL_KEY.getFieldName(), searchString)));
          } else {
            query.addCustomGroupings(ViewsMetaDataFields.LABEL_VALUE.getAlias());
            query.addCondition(getCondition(getLabelKeyFilter(new String[] {viewFieldInput.getFieldName()})));
            query.addAliasedColumn(new CustomSql(String.format(distinct, viewFieldInput.getFieldId())),
                ViewsMetaDataFields.LABEL_VALUE.getAlias());
            query.addCondition(new CustomCondition(
                String.format(searchFilter, ViewsMetaDataFields.LABEL_VALUE.getFieldName(), searchString)));
          }
          query.addCustomJoin(leftJoinLabels);
          break;
        case CUSTOM:
          ViewCustomField customField = viewCustomFieldDao.getById(viewFieldInput.getFieldId());
          List<String> labelsKeysList = getLabelsKeyList(customField);
          List<String> listOfNotNullEntities = new ArrayList<>();

          if (!labelsKeysList.isEmpty()) {
            List<ViewField> customFieldViewFields = customField.getViewFields();
            for (ViewField viewField : customFieldViewFields) {
              if (viewField.getIdentifier() != ViewFieldIdentifier.LABEL) {
                listOfNotNullEntities.add(viewField.getFieldId());
              }
            }
            decorateQueryWithLabelsMetadata(query, true, false, Collections.emptyList());
            String[] labelsKeysListStringArray = labelsKeysList.toArray(new String[labelsKeysList.size()]);

            List<Condition> conditionList = new ArrayList<>();
            for (String fieldId : listOfNotNullEntities) {
              conditionList.add(UnaryCondition.isNotNull(new CustomSql(fieldId)));
            }
            conditionList.add(getCondition(getLabelKeyFilter(labelsKeysListStringArray)));
            query.addCondition(getSqlOrCondition(conditionList));
          }
          query.addAliasedColumn(new CustomSql(String.format(distinct, customField.getSqlFormula())),
              modifyStringToComplyRegex(customField.getName()));
          query.addCondition(
              new CustomCondition(String.format(searchFilter, customField.getSqlFormula(), searchString)));
          break;
        default:
          throw new InvalidRequestException("Invalid View Field Identifier " + viewFieldInput.getIdentifier());
      }
      fields.add(filter.getField());
    }
    log.info("Query for view filter {}", query.toString());

    return ViewsQueryMetadata.builder().query(query).fields(fields).build();
  }

  private QLCEViewFilter getLabelKeyFilter(String[] values) {
    return QLCEViewFilter.builder()
        .field(QLCEViewFieldInput.builder()
                   .fieldId(LABEL_KEY.getFieldName())
                   .identifier(ViewFieldIdentifier.LABEL)
                   .identifierName(ViewFieldIdentifier.LABEL.getDisplayName())
                   .build())
        .operator(QLCEViewFilterOperator.IN)
        .values(values)
        .build();
  }

  private void decorateQueryWithLabelsMetadata(
      SelectQuery selectQuery, boolean isLabelsPresent, boolean labelGroupByPresent, List<String> labelKeyList) {
    if (isLabelsPresent || labelGroupByPresent) {
      if (labelKeyList.isEmpty()) {
        selectQuery.addCustomJoin(leftJoinLabels);
      } else {
        selectQuery.addCustomJoin(String.format(leftJoinSelectiveLabels, processLabelKeyList(labelKeyList)));
      }
    }
    if (labelGroupByPresent) {
      selectQuery.addCustomGroupings(ViewsMetaDataFields.LABEL_VALUE.getAlias());
      selectQuery.addCustomColumns(Converter.toCustomColumnSqlObject(
          ViewsMetaDataFields.LABEL_VALUE.getFieldName(), ViewsMetaDataFields.LABEL_VALUE.getAlias()));
    }
  }

  private String processLabelKeyList(List<String> labelKeyList) {
    labelKeyList.replaceAll(labelKey -> String.format("'%s'", labelKey));
    return String.join(",", labelKeyList);
  }

  private List<String> modifyQueryForCustomFields(SelectQuery selectQuery, List<ViewField> customFields) {
    List<String> labelsKeysListAcrossCustomFields = new ArrayList<>();
    List<String> listOfNotNullEntities = new ArrayList<>();
    for (ViewField field : customFields) {
      ViewCustomField customField = viewCustomFieldDao.getById(field.getFieldId());
      final List<String> labelsKeysList = getLabelsKeyList(customField);
      labelsKeysListAcrossCustomFields.addAll(labelsKeysList);
      if (!labelsKeysList.isEmpty()) {
        List<ViewField> customFieldViewFields = customField.getViewFields();
        for (ViewField viewField : customFieldViewFields) {
          if (viewField.getIdentifier() != ViewFieldIdentifier.LABEL) {
            listOfNotNullEntities.add(viewField.getFieldId());
          }
        }
      }
    }
    if (!labelsKeysListAcrossCustomFields.isEmpty()) {
      String[] labelsKeysListAcrossCustomFieldsStringArray =
          labelsKeysListAcrossCustomFields.toArray(new String[labelsKeysListAcrossCustomFields.size()]);

      List<Condition> conditionList = new ArrayList<>();
      for (String fieldId : listOfNotNullEntities) {
        conditionList.add(UnaryCondition.isNotNull(new CustomSql(fieldId)));
      }
      conditionList.add(new InCondition(
          new CustomSql(LABEL_KEY.getFieldName()), (Object[]) labelsKeysListAcrossCustomFieldsStringArray));
      selectQuery.addCondition(getSqlOrCondition(conditionList));
    }
    return labelsKeysListAcrossCustomFields;
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
    } else if (operator.equals(ViewIdOperator.NOT_NULL)) {
      return QLCEViewFilterOperator.NOT_NULL;
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
        .identifierName(field.getIdentifier().getDisplayName())
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

      if (filter.getOperator() == QLCEViewFilterOperator.NOT_NULL) {
        return BinaryCondition.equalTo(LABEL_KEY.getFieldName(), labelKey);
      }

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
      case NOT_NULL:
        return UnaryCondition.isNotNull(conditionKey);
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
        if (field.getFieldId().equals(LABEL_KEY.getFieldName())) {
          return LABEL_KEY.getAlias();
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
