package software.wings.graphql.datafetcher.environment;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Environment;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTypeFilter;

import java.util.List;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
public class EnvironmentQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLEnvironmentFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<Environment>> field;

      if (filter.getApplication() != null) {
        field = query.field("appId");
        QLIdFilter applicationFilter = filter.getApplication();
        utils.setIdFilter(field, applicationFilter);
      }

      if (filter.getEnvironment() != null) {
        field = query.field("_id");
        QLIdFilter environmentFilter = filter.getEnvironment();
        utils.setIdFilter(field, environmentFilter);
      }

      if (filter.getEnvironmentType() != null) {
        field = query.field("environmentType");
        QLEnvironmentTypeFilter envTypeFilter = filter.getEnvironmentType();
        utils.setEnumFilter(field, envTypeFilter);
      }
    });
  }
}
