package software.wings.graphql.datafetcher.user;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLUserQueryParameters;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.QLUser.QLUserBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class UserDataFetcher extends AbstractObjectDataFetcher<QLUser, QLUserQueryParameters> {
  public static final String USER_DOES_NOT_EXIST_MSG = "User does not exist";
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_READ)
  public QLUser fetch(QLUserQueryParameters qlQuery, String accountId) {
    User user = null;
    if (qlQuery.getId() != null) {
      user = persistence.get(User.class, qlQuery.getId());
    }
    if (qlQuery.getName() != null) {
      try (HIterator<User> iterator =
               new HIterator<>(persistence.createQuery(User.class).filter(UserKeys.name, qlQuery.getName()).fetch())) {
        if (iterator.hasNext()) {
          user = iterator.next();
        }
      }
    }
    if (user == null) {
      throw new InvalidRequestException(USER_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
    final QLUserBuilder builder = QLUser.builder();
    UserController.populateUser(user, builder);
    return builder.build();
  }
}
