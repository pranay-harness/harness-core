package software.wings.graphql.datafetcher.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.service.QLServiceFilter;

import java.util.List;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
public class ServiceQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLServiceFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Service>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getService() != null) {
        field = query.field("_id");
        QLIdFilter serviceFilter = filter.getService();
        utils.setIdFilter(field, serviceFilter);
      }
    });
  }
}
