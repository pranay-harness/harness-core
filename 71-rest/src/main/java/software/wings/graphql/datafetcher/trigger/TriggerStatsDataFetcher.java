package software.wings.graphql.datafetcher.trigger;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.RealTimeStatsDataFetcher;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerAggregation;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerEntityAggregation;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerFilter;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.service.intfc.AppService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TriggerStatsDataFetcher extends RealTimeStatsDataFetcher<QLNoOpAggregateFunction, QLTriggerFilter,
    QLTriggerAggregation, QLNoOpSortCriteria> {
  @Inject private AppService appService;
  @Inject TriggerQueryHelper triggerQueryHelper;

  @Override
  protected QLData fetch(String accountId, QLNoOpAggregateFunction aggregateFunction, List<QLTriggerFilter> filters,
      List<QLTriggerAggregation> groupBy, List<QLNoOpSortCriteria> sortCriteria) {
    final Class entityClass = Trigger.class;
    List<String> groupByList = new ArrayList<>();
    if (isNotEmpty(groupBy)) {
      groupByList = groupBy.stream()
                        .filter(g -> g != null && g.getEntityAggregation() != null)
                        .map(g -> g.getEntityAggregation().name())
                        .collect(Collectors.toList());
    }
    return getQLData(accountId, filters, entityClass, groupByList);
  }

  private void populateAppIdFilter(String accountId, Query query) {
    List<String> appIds = appService.getAppIdsByAccountId(accountId);
    query.field(TriggerKeys.appId).in(appIds);
  }

  @Override
  @NotNull
  protected Query populateFilters(
      WingsPersistence wingsPersistence, String accountId, List<QLTriggerFilter> filters, Class entityClass) {
    Query query = wingsPersistence.createQuery(entityClass);
    populateFilters(accountId, filters, query);
    populateAppIdFilter(accountId, query);
    return query;
  }

  protected String getAggregationFieldName(String aggregation) {
    QLTriggerEntityAggregation triggerAggregation = QLTriggerEntityAggregation.valueOf(aggregation);
    switch (triggerAggregation) {
      case Application:
        return "appId";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  @Override
  protected void populateFilters(String accountId, List<QLTriggerFilter> filters, Query query) {
    triggerQueryHelper.setQuery(filters, query);
  }

  @Override
  public String getEntityType() {
    return NameService.trigger;
  }
}
