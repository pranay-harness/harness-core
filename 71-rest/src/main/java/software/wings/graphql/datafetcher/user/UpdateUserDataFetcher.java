package software.wings.graphql.datafetcher.user;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.utils.RequestField;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.userGroup.UserGroupController;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.user.QLUpdateUserInput;
import software.wings.graphql.schema.type.user.QLUpdateUserPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class UpdateUserDataFetcher extends BaseMutatorDataFetcher<QLUpdateUserInput, QLUpdateUserPayload> {
  @Inject private UserService userService;
  @Inject private UserGroupController userGroupController;
  private final String INVALID_INP_ERR_MSSG = "cannot be empty or blank";

  @Inject
  public UpdateUserDataFetcher(UserService userService) {
    super(QLUpdateUserInput.class, QLUpdateUserPayload.class);
    this.userService = userService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLUpdateUserPayload mutateAndFetch(QLUpdateUserInput qlUpdateUserInput, MutationContext mutationContext) {
    validate(qlUpdateUserInput);
    String existingUserId = qlUpdateUserInput.getId();
    User existingUser;
    try {
      existingUser = userService.get(existingUserId);
    } catch (UnauthorizedException ex) {
      throw new InvalidRequestException("User not found", ex);
    }
    final User updatedUser =
        userService.update(prepareUserToUpdate(qlUpdateUserInput, existingUser, mutationContext.getAccountId()));
    return QLUpdateUserPayload.builder()
        .clientMutationId(qlUpdateUserInput.getClientMutationId())
        .user(prepareQLUser(updatedUser))
        .build();
  }

  private void validate(QLUpdateUserInput qlUpdateUserInput) {
    if (StringUtils.isBlank(qlUpdateUserInput.getId())) {
      throw new InvalidArgumentsException(Pair.of("id", INVALID_INP_ERR_MSSG));
    }

    final RequestField<List<String>> userGroupIds = qlUpdateUserInput.getUserGroupIds();
    if (isInitialized(userGroupIds) && getValue(userGroupIds).orElse(Collections.emptyList()).contains("")) {
      throw new InvalidArgumentsException(Pair.of("userGroupId", INVALID_INP_ERR_MSSG));
    }
  }

  private User prepareUserToUpdate(QLUpdateUserInput qlUpdateUserInput, User existingUser, final String accountId) {
    if (isInitialized(qlUpdateUserInput.getName())) {
      final String name = getValue(qlUpdateUserInput.getName()).orElse(null);
      existingUser.setName(name);
    }
    if (isInitialized(qlUpdateUserInput.getUserGroupIds())) {
      final List<String> userGroupIds = getValue(qlUpdateUserInput.getUserGroupIds()).orElse(null);
      userGroupController.addUserToUserGroups(existingUser, userGroupIds, accountId);
    }
    return existingUser;
  }

  private boolean isInitialized(RequestField<?> field) {
    return field != null && field.hasBeenSet();
  }

  private <T> Optional<T> getValue(RequestField<T> obj) {
    return obj.getValue();
  }

  private QLUser prepareQLUser(User savedUser) {
    return UserController.populateUser(savedUser, QLUser.builder());
  }
}
