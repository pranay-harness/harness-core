package software.wings.graphql.datafetcher.trigger;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerAggregation;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerFilter;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerFilterType;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.service.intfc.AppService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TriggerStatsDataFetcher extends RealTimeStatsDataFetcher<QLAggregateFunction, QLTriggerFilter,
    QLTriggerAggregation, QLTimeSeriesAggregation, QLNoOpSortCriteria> {
  @Inject private AppService appService;

  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLTriggerFilter> filters,
      List<QLTriggerAggregation> groupBy, QLTimeSeriesAggregation groupByTime, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Trigger.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream().map(g -> g.name()).collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  @NotNull
  protected Query populateAccountFilter(String accountId, Class entityClass) {
    Query query = wingsPersistence.createQuery(entityClass);
    List<String> appIds = appService.getAppIdsByAccountId(accountId);
    query.field(TriggerKeys.appId).in(appIds);
    return query;
  }

  protected String getFilterFieldName(String filterType) {
    QLTriggerFilterType triggerFilterType = QLTriggerFilterType.valueOf(filterType);
    switch (triggerFilterType) {
      case Application:
        return "appId";
      case Trigger:
        return "_id";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }

  protected String getAggregationFieldName(String aggregation) {
    QLTriggerAggregation triggerAggregation = QLTriggerAggregation.valueOf(aggregation);
    switch (triggerAggregation) {
      case Application:
        return "appId";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  protected String getAggregationNameField(String aggregation) {
    QLTriggerAggregation triggerAggregation = QLTriggerAggregation.valueOf(aggregation);
    switch (triggerAggregation) {
      case Application:
        return "appName";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  public String getEntityType() {
    return NameService.trigger;
  }
}
