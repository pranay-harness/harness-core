package software.wings.graphql.datafetcher.trigger;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.Trigger.TriggerKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerFilter;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerFilterType;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.graphql.schema.type.trigger.QLTriggerConnection;
import software.wings.graphql.schema.type.trigger.QLTriggerConnection.QLTriggerConnectionBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class TriggerConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLTriggerFilter, QLNoOpSortCriteria, QLTriggerConnection> {
  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN, action = Action.READ)
  protected QLTriggerConnection fetchConnection(List<QLTriggerFilter> triggerFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Trigger> query = populateFilters(wingsPersistence, triggerFilters, Trigger.class);
    query.order(Sort.descending(TriggerKeys.createdAt));

    QLTriggerConnectionBuilder qlTriggerConnectionBuilder = QLTriggerConnection.builder();
    qlTriggerConnectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, trigger -> {
      QLTriggerBuilder builder = QLTrigger.builder();
      TriggerController.populateTrigger(trigger, builder);
      qlTriggerConnectionBuilder.node(builder.build());
    }));

    return qlTriggerConnectionBuilder.build();
  }

  protected String getFilterFieldName(String filterType) {
    QLTriggerFilterType triggerFilterType = QLTriggerFilterType.valueOf(filterType);
    switch (triggerFilterType) {
      case Application:
        return TriggerKeys.appId;
      case Trigger:
        return TriggerKeys.uuid;
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }

  /**
   *
   * @return
   */
  @Override
  public String getAccountId() {
    return null;
  }
}
