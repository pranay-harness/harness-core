package software.wings.graphql.datafetcher.trigger;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerAggregation;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerFilter;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.service.intfc.AppService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TriggerStatsDataFetcher extends RealTimeStatsDataFetcher<QLNoOpAggregateFunction, QLTriggerFilter,
    QLTriggerAggregation, QLTimeSeriesAggregation, QLNoOpSortCriteria> {
  @Inject private AppService appService;
  @Inject TriggerQueryHelper triggerQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLTriggerFilter> filters,
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

  protected String getAggregationFieldName(String aggregation) {
    QLTriggerAggregation triggerAggregation = QLTriggerAggregation.valueOf(aggregation);
    switch (triggerAggregation) {
      case Application:
        return "appId";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  protected void populateFilters(List<QLTriggerFilter> filters, Query query) {
    triggerQueryHelper.setQuery(filters, query);
  }

  @Override
  public String getEntityType() {
    return NameService.trigger;
  }
}
