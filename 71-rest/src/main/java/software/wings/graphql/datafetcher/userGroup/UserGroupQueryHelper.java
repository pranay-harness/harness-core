package software.wings.graphql.datafetcher.userGroup;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.usergroup.QLUserGroupFilter;

import java.util.List;

@Singleton
@Slf4j
public class UserGroupQueryHelper {
  @Inject private DataFetcherUtils utils;

  public void setQuery(List<QLUserGroupFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<UserGroup>> field;

      if (filter.getUser() != null) {
        field = query.field("memberIds");
        QLIdFilter userFilter = filter.getUser();
        utils.setIdFilter(field, userFilter);
      }
    });
  }
}
