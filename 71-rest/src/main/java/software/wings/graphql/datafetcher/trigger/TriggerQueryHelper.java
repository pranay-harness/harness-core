package software.wings.graphql.datafetcher.trigger;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.trigger.QLTriggerFilter;

import java.util.List;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
public class TriggerQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLTriggerFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Trigger>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getTrigger() != null) {
        field = query.field("_id");
        QLIdFilter triggerFilter = filter.getTrigger();
        utils.setIdFilter(field, triggerFilter);
      }
    });
  }
}
