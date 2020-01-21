package software.wings.graphql.datafetcher.user;

import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.QLUser.QLUserBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpFilterCriteria;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.user.QLUserConnection;
import software.wings.graphql.schema.type.user.QLUserConnection.QLUserConnectionBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class UserConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLNoOpFilterCriteria, QLNoOpSortCriteria, QLUserConnection> {
  @Override
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_READ)
  public QLUserConnection fetchConnection(List<QLNoOpFilterCriteria> filters, QLPageQueryParameters pageQueryParameters,
      List<QLNoOpSortCriteria> sortCriteria) {
    Query<User> query = populateFilters(wingsPersistence, filters, User.class, false);
    query.order(Sort.ascending(UserKeys.name));

    QLUserConnectionBuilder connectionBuilder = QLUserConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, user -> {
      QLUserBuilder builder = QLUser.builder();
      QLUser qlUser = UserController.populateUser(user, builder);
      connectionBuilder.node(qlUser);
    }));
    return connectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLNoOpFilterCriteria> filters, Query query) {
    // NO_OP
  }

  @Override
  protected QLNoOpFilterCriteria generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }
}
