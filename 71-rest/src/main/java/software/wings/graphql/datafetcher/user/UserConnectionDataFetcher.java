package software.wings.graphql.datafetcher.user;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.UnexpectedException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.QLUser.QLUserBuilder;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.user.QLUserFilter;
import software.wings.graphql.schema.type.user.QLUserConnection;
import software.wings.graphql.schema.type.user.QLUserConnection.QLUserConnectionBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
public class UserConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLUserFilter, QLNoOpSortCriteria, QLUserConnection> {
  private UserGroupService userGroupService;
  private UserQueryHelper userQueryHelper;

  @Inject
  public UserConnectionDataFetcher(UserGroupService userGroupService, UserQueryHelper userQueryHelper) {
    this.userGroupService = userGroupService;
    this.userQueryHelper = userQueryHelper;
  }

  @Override
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_READ)
  public QLUserConnection fetchConnection(
      List<QLUserFilter> filters, QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
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
  protected void populateFilters(List<QLUserFilter> filters, Query query) {
    userQueryHelper.setQuery(filters, query);
  }

  @Override
  protected QLUserFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    if ("userGroup".equals(key)) {
      final List<String> userIdsInUserGroup =
          getUserIdsInUserGroup((String) utils.getFieldValue(environment.getSource(), value));
      if (!userIdsInUserGroup.isEmpty()) {
        return QLUserFilter.builder()
            .user(QLIdFilter.builder()
                      .operator(QLIdOperator.IN)
                      .values(userIdsInUserGroup.toArray(new String[0]))
                      .build())
            .build();
      }
      // this is to handle casde when user group does not have any user. We should not return any user then
      return QLUserFilter.builder()
          .user(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"DUMMY_USER_ID"}).build())
          .build();
    }
    throw new UnexpectedException("Unsupported field " + key + " while generating filter");
  }

  private List<String> getUserIdsInUserGroup(String userGroupId) {
    return Optional.ofNullable(userGroupService.get(getAccountId(), userGroupId, false))
        .map(UserGroup::getMemberIds)
        .orElse(Collections.emptyList());
  }
}
