package software.wings.graphql.datafetcher.cloudefficiencyevents;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.fabric8.utils.Lists;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsMetaDataFields;
import software.wings.graphql.datafetcher.cloudefficiencyevents.CEEventsQueryMetaData.CEEventsQueryMetaDataBuilder;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class EventsDataQueryBuilder {
  private CEEventsTableSchema schema = new CEEventsTableSchema();

  protected CEEventsQueryMetaData formQuery(
      String accountId, List<QLEventsDataFilter> filters, List<QLEventsSortCriteria> sortCriteria) {
    CEEventsQueryMetaDataBuilder queryMetaDataBuilder = CEEventsQueryMetaData.builder();
    SelectQuery selectQuery = new SelectQuery();

    List<CEEventsMetaDataFields> fieldNames = new ArrayList<>();

    selectQuery.addCustomFromTable(schema.getCeEventsTable());

    if (!Lists.isNullOrEmpty(filters)) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    List<QLEventsSortCriteria> finalSortCriteria = validateAndAddSortCriteria(selectQuery, sortCriteria);

    addAccountFilter(selectQuery, accountId);

    // Adding Columns we want to read from the table
    selectQuery.addColumns(schema.getStartTime());
    selectQuery.addColumns(schema.getEventDescription());
    selectQuery.addColumns(schema.getCostEventSource());
    selectQuery.addColumns(schema.getCostEventType());
    fieldNames.add(CEEventsMetaDataFields.valueOf(schema.getStartTime().getName().toUpperCase()));
    fieldNames.add(CEEventsMetaDataFields.valueOf(schema.getEventDescription().getName().toUpperCase()));
    fieldNames.add(CEEventsMetaDataFields.valueOf(schema.getCostEventSource().getName().toUpperCase()));
    fieldNames.add(CEEventsMetaDataFields.valueOf(schema.getCostEventType().getName().toUpperCase()));

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.sortCriteria(finalSortCriteria);
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  private List<QLEventsSortCriteria> validateAndAddSortCriteria(
      SelectQuery selectQuery, List<QLEventsSortCriteria> sortCriteria) {
    if (isEmpty(sortCriteria)) {
      return sortCriteria;
    } else {
      for (QLEventsSortCriteria eventsSortCriteria : sortCriteria) {
        appendSortToQuery(selectQuery, eventsSortCriteria);
      }
    }
    return sortCriteria;
  }

  private void appendSortToQuery(SelectQuery selectQuery, QLEventsSortCriteria sortCriteria) {
    QLEventsSortType sortType = sortCriteria.getSortType();
    OrderObject.Dir dir =
        sortCriteria.getSortOrder() == QLSortOrder.ASCENDING ? OrderObject.Dir.ASCENDING : OrderObject.Dir.DESCENDING;
    if (sortType == QLEventsSortType.Time) {
      selectQuery.addCustomOrdering(CEEventsMetaDataFields.STARTTIME.getFieldName(), dir);
    }
  }

  private void decorateQueryWithFilters(SelectQuery selectQuery, List<QLEventsDataFilter> filters) {
    for (QLEventsDataFilter filter : filters) {
      Set<QLEventsDataFilterType> filterTypes = QLEventsDataFilter.getFilterTypes(filter);
      for (QLEventsDataFilterType type : filterTypes) {
        if (type.getMetaDataFields().getFilterKind() == QLFilterKind.SIMPLE) {
          decorateSimpleFilter(selectQuery, filter, type);
        } else {
          logger.error("Failed to apply filter :[{}]", filter);
        }
      }
    }
  }

  private void decorateSimpleFilter(SelectQuery selectQuery, QLEventsDataFilter filter, QLEventsDataFilterType type) {
    Filter f = QLEventsDataFilter.getFilter(type, filter);
    if (checkFilter(f)) {
      if (isIdFilter(f)) {
        addSimpleIdOperator(selectQuery, f, type);
      } else if (isTimeFilter(f)) {
        addSimpleTimeFilter(selectQuery, f, type);
      }
    } else {
      logger.info("Not adding filter since it is not valid " + f);
    }
  }

  private void addSimpleTimeFilter(SelectQuery selectQuery, Filter filter, QLEventsDataFilterType type) {
    DbColumn key = getFilterKey(type);
    QLTimeFilter typeCastedFilter = (QLTimeFilter) filter;
    QLTimeFilter timeFilter = typeCastedFilter;
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

  private void addSimpleIdOperator(SelectQuery selectQuery, Filter filter, QLEventsDataFilterType type) {
    DbColumn key = getFilterKey(type);
    QLIdOperator operator = (QLIdOperator) filter.getOperator();
    QLIdOperator finalOperator = operator;
    if (filter.getValues().length > 0) {
      if (operator == QLIdOperator.EQUALS) {
        finalOperator = QLIdOperator.IN;
      } else {
        finalOperator = operator;
      }
    }
    switch (finalOperator) {
      case NOT_NULL:
        selectQuery.addCondition(UnaryCondition.isNotNull(key));
        break;
      case NOT_IN:
        InCondition inCondition = new InCondition(key, (Object[]) filter.getValues());
        inCondition.setNegate(true);
        selectQuery.addCondition(inCondition);
        break;
      case IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getValues()));
        break;
      case EQUALS:
        selectQuery.addCondition(BinaryCondition.equalTo(key, filter.getValues()[0]));
        break;
      default:
        throw new InvalidRequestException("String simple operator not supported" + operator);
    }
  }

  private boolean isTimeFilter(Filter f) {
    return f instanceof QLTimeFilter;
  }

  private boolean isIdFilter(Filter f) {
    return f instanceof QLIdFilter;
  }

  private boolean checkFilter(Filter f) {
    return f.getOperator() != null && EmptyPredicate.isNotEmpty(f.getValues());
  }

  private DbColumn getFilterKey(QLEventsDataFilterType type) {
    switch (type) {
      case EndTime:
      case StartTime:
        return schema.getStartTime();
      case Application:
        return schema.getAppId();
      case Service:
        return schema.getServiceId();
      case Environment:
        return schema.getEnvId();
      case Cluster:
        return schema.getClusterId();
      case CloudServiceName:
        return schema.getCloudServiceName();
      case TaskId:
        return schema.getTaskId();
      case WorkloadName:
        return schema.getWorkloadName();
      case Namespace:
        return schema.getNamespace();
      default:
        throw new InvalidRequestException("Filter type not supported " + type);
    }
  }

  private void addAccountFilter(SelectQuery selectQuery, String accountId) {
    selectQuery.addCondition(BinaryCondition.equalTo(schema.getAccountId(), accountId));
  }
}
