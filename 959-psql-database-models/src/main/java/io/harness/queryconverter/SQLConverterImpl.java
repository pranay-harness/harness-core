/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.queryconverter;

import static com.google.common.base.MoreObjects.firstNonNull;

import io.harness.queryconverter.dto.AggregationOperation;
import io.harness.queryconverter.dto.FieldAggregation;
import io.harness.queryconverter.dto.FieldFilter;
import io.harness.queryconverter.dto.GridRequest;
import io.harness.queryconverter.dto.SortCriteria;
import io.harness.queryconverter.dto.SortOrder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectField;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;

@Slf4j
@Singleton
public class SQLConverterImpl implements SQLConverter {
  private final DSLContext dsl;

  @Inject
  public SQLConverterImpl(DSLContext dsl) {
    this.dsl = dsl;
  }

  @Override
  public List<? extends Serializable> convert(@NonNull GridRequest request) throws Exception {
    return convert(request.getEntity(), request);
  }

  @Override
  public List<? extends Serializable> convert(@NonNull String tableName, GridRequest request) throws Exception {
    Table<?> jooqTable = SQLConverter.getTable(tableName);
    Class<? extends Serializable> pojoClass = RecordToPojo.valueOf(jooqTable.getName().toUpperCase()).getPojoClazz();

    return convert(jooqTable, request, pojoClass);
  }

  @Override
  public List<? extends Serializable> convert(Table<?> jooqTable, GridRequest request) throws Exception {
    Class<? extends Serializable> pojoClass = RecordToPojo.valueOf(jooqTable.getName().toUpperCase()).getPojoClazz();

    return convert(jooqTable, request, pojoClass);
  }

  @Override
  public List<? extends Serializable> convert(
      String tableName, GridRequest request, Class<? extends Serializable> fetchInto) throws Exception {
    Table<?> jooqTable = SQLConverter.getTable(tableName);

    return convert(jooqTable, request, fetchInto);
  }

  @Override
  public List<? extends Serializable> convert(
      Table<?> jooqTable, GridRequest request, Class<? extends Serializable> fetchInto) throws Exception {
    Integer offset = firstNonNull(request.getOffset(), 0);
    Integer limit = request.getLimit();

    Condition filterCondition = getFilters(request.getWhere(), jooqTable);

    List<Field<?>> groupByFields = getGroupBy(request.getGroupBy(), jooqTable);
    List<SortField<?>> sortFields = getSortFields(request.getOrderBy(), jooqTable);
    List<SelectField<?>> aggregationFields = getAggregation(request.getAggregate(), jooqTable);

    if (groupByFields.isEmpty() && aggregationFields.isEmpty()) {
      return dsl.selectFrom(jooqTable)
          .where(filterCondition)
          .orderBy(sortFields)
          .offset(offset)
          .limit(limit)
          .fetchInto(fetchInto);
    }

    List<SelectField<?>> selectFields = new ArrayList<>(aggregationFields);
    Condition havingCondition = getFilters(request.getHaving(), jooqTable);

    if (groupByFields.isEmpty()) {
      return dsl.select(selectFields)
          .from(jooqTable)
          .where(filterCondition)
          .having(havingCondition)
          .orderBy(sortFields)
          .offset(offset)
          .limit(limit)
          .fetchInto(fetchInto);
    }

    selectFields.addAll(groupByFields);

    return dsl.select(selectFields)
        .from(jooqTable)
        .where(filterCondition)
        .groupBy(groupByFields)
        .having(havingCondition)
        .orderBy(sortFields)
        .offset(offset)
        .limit(limit)
        .fetchInto(fetchInto);
  }

  private List<SortField<?>> getSortFields(List<SortCriteria> sortCriteriaList, Table<?> tableName)
      throws InvalidPropertiesFormatException {
    List<SortField<?>> result = new ArrayList<>();
    for (SortCriteria sortCriteria : sortCriteriaList) {
      if (sortCriteria != null) {
        Field<?> tableFieldName = SQLConverter.getField(sortCriteria.getField(), tableName);
        if (sortCriteria.getOrder() == SortOrder.DESCENDING || sortCriteria.getOrder() == SortOrder.DESC) {
          result.add(tableFieldName.as(tableFieldName.getUnqualifiedName()).desc());
        } else if (sortCriteria.getOrder() == SortOrder.ASCENDING || sortCriteria.getOrder() == SortOrder.ASC) {
          result.add(tableFieldName.as(tableFieldName.getUnqualifiedName()).asc());
        } else {
          throw new InvalidPropertiesFormatException(
              "sortCriteria.order = [" + sortCriteria.getOrder() + "] not implemented");
        }
      }
    }
    return result;
  }

  private List<Field<?>> getGroupBy(List<String> groupBy, Table<?> tableName) {
    return groupBy.stream()
        .map(columnName -> SQLConverter.getField(columnName, tableName))
        .collect(Collectors.toList());
  }

  @SneakyThrows
  private List<SelectField<?>> getAggregation(List<FieldAggregation> aggregationFunctionList, Table<?> tableName) {
    List<SelectField<?>> result = new ArrayList<>();

    for (FieldAggregation fieldAggregation : aggregationFunctionList) {
      if (fieldAggregation.getOperation() == AggregationOperation.COUNT) {
        if (fieldAggregation.getField() == null) {
          result.add(DSL.count());
        } else {
          result.add(DSL.count().as(fieldAggregation.getField()));
        }
        continue;
      }

      Field<?> tableFieldName = SQLConverter.getField(fieldAggregation.getField(), tableName);
      switch (fieldAggregation.getOperation()) {
        case SUM:
          result.add(DSL.sum((Field<Number>) tableFieldName).as(tableFieldName));
          break;
        case AVG:
          result.add(DSL.avg((Field<Number>) tableFieldName).as(tableFieldName));
          break;
        case MAX:
          result.add(DSL.max(tableFieldName).as(tableFieldName));
          break;
        case MIN:
          result.add(DSL.min(tableFieldName).as(tableFieldName));
          break;
        default:
          throw new InvalidPropertiesFormatException(
              "fieldAggregation.operation = [" + fieldAggregation.getOperation() + "] is not implemented");
      }
    }
    return result;
  }

  private static OffsetDateTime getOffsetDateTimeFromLong(String value) {
    final Instant instant = Instant.ofEpochMilli(Long.parseLong(value, 10));
    return OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
  }

  @SneakyThrows
  private Condition getFilters(List<FieldFilter> filters, Table<?> jooqTable) {
    Condition condition = DSL.noCondition();

    for (FieldFilter filter : filters) {
      Field<?> tableFieldName = SQLConverter.getField(filter.getField(), jooqTable);
      switch (filter.getOperator()) {
        case IN:
        case EQUALS:
          condition = condition.and(tableFieldName.in(filter.getValues()));
          break;
        case NOT_IN:
          condition = condition.and(tableFieldName.notIn(filter.getValues()));
          break;
        case NOT_NULL:
          condition = condition.and(tableFieldName.isNotNull());
          break;
        case LIKE:
          condition = condition.and(tableFieldName.like(filter.getValues().get(0)));
          break;
        case TIME_AFTER:
          final OffsetDateTime offsetDateTime1 = getOffsetDateTimeFromLong(filter.getValues().get(0));
          condition = condition.and(((Field<OffsetDateTime>) tableFieldName.as(tableFieldName.getUnqualifiedName()))
                                        .greaterOrEqual(offsetDateTime1));
          break;
        case TIME_BEFORE:
          final OffsetDateTime offsetDateTime2 = getOffsetDateTimeFromLong(filter.getValues().get(0));
          condition = condition.and(((Field<OffsetDateTime>) tableFieldName.as(tableFieldName.getUnqualifiedName()))
                                        .lessOrEqual(offsetDateTime2));
          break;
        case GREATER_OR_EQUALS:
          condition = condition.and(
              ((Field<Number>) tableFieldName).greaterOrEqual(Double.parseDouble(filter.getValues().get(0))));
          break;
        case LESS_OR_EQUALS:
          condition = condition.and(
              ((Field<Number>) tableFieldName).lessOrEqual(Double.parseDouble(filter.getValues().get(0))));
          break;
        default:
          throw new InvalidPropertiesFormatException(
              "FieldFilter.operator = [" + filter.getOperator() + "] is not implemented");
      }
    }
    return condition;
  }
}
