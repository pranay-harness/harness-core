package software.wings.graphql.datafetcher.connector;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorFilter;
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorTypeFilter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;

/**
 * @author rktummala on 07/12/19
 */
@Singleton
public class ConnectorQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLConnectorFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<SettingAttribute>> field;

      if (filter.getConnector() != null) {
        field = query.field("_id");
        QLIdFilter connectorFilter = filter.getConnector();
        utils.setIdFilter(field, connectorFilter);
      }

      if (filter.getConnectorType() != null) {
        field = query.field("value.type");
        QLConnectorTypeFilter connectorTypeFilter = filter.getConnectorType();
        utils.setEnumFilter(field, connectorTypeFilter);
      }

      if (filter.getCreatedAt() != null) {
        field = query.field("createdAt");
        QLTimeFilter createdAtFilter = filter.getCreatedAt();
        utils.setTimeFilter(field, createdAtFilter);
      }
    });
  }
}
