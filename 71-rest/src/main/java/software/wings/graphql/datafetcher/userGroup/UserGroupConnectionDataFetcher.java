package software.wings.graphql.datafetcher.userGroup;

import static software.wings.graphql.utils.nameservice.NameService.user;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.GraphQLException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.usergroup.QLUserGroup;
import software.wings.graphql.schema.type.usergroup.QLUserGroup.QLUserGroupBuilder;
import software.wings.graphql.schema.type.usergroup.QLUserGroupConnection;
import software.wings.graphql.schema.type.usergroup.QLUserGroupConnection.QLUserGroupConnectionBuilder;
import software.wings.graphql.schema.type.usergroup.QLUserGroupFilter;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class UserGroupConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLUserGroupFilter, QLNoOpSortCriteria, QLUserGroupConnection> {
  @Inject UserGroupController userGroupController;
  @Inject UserGroupQueryHelper userGroupQueryHelper;
  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_READ)
  public QLUserGroupConnection fetchConnection(List<QLUserGroupFilter> groupFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<UserGroup> query = populateFilters(wingsPersistence, groupFilters, UserGroup.class, true)
                                 .order(Sort.ascending(UserGroupKeys.name));

    QLUserGroupConnectionBuilder connectionBuilder = QLUserGroupConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, userGroup -> {
      QLUserGroupBuilder builder = QLUserGroup.builder();
      userGroupController.populateUserGroupOutput(userGroup, builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLUserGroupFilter> filters, Query query) {
    userGroupQueryHelper.setQuery(filters, query);
  }

  @Override
  protected QLUserGroupFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    QLIdFilter idFilter = QLIdFilter.builder()
                              .operator(QLIdOperator.EQUALS)
                              .values(new String[] {(String) utils.getFieldValue(environment.getSource(), value)})
                              .build();
    if (user.equals(key)) {
      return QLUserGroupFilter.builder().user(idFilter).build();
    }
    throw new GraphQLException("Unsupported field " + key + " while generating filter", WingsException.SRE);
  }
}
