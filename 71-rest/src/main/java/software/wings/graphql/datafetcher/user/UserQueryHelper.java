package software.wings.graphql.datafetcher.user;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.user.QLUserFilter;

import java.util.List;

@Singleton
public class UserQueryHelper {
  private DataFetcherUtils utils;

  @Inject
  public UserQueryHelper(DataFetcherUtils utils) {
    this.utils = utils;
  }
  public void setQuery(List<QLUserFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }
    filters.forEach(filter -> {
      if (filter.getUser() != null) {
        FieldEnd<? extends Query<User>> field = query.field("_id");
        QLIdFilter userFilter = filter.getUser();
        utils.setIdFilter(field, userFilter);
      }
    });
  }
}
