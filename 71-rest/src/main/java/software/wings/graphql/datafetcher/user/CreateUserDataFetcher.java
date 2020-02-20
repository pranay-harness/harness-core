package software.wings.graphql.datafetcher.user;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import io.harness.utils.RequestField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInviteSource;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.datafetcher.userGroup.UserGroupController;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.user.QLCreateUserInput;
import software.wings.graphql.schema.type.user.QLCreateUserPayload;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class CreateUserDataFetcher extends BaseMutatorDataFetcher<QLCreateUserInput, QLCreateUserPayload> {
  @Inject private UserService userService;
  @Inject UserGroupController userGroupController;
  private final String INVALID_VAL_INP_PARAM_ERR_MSSG = "cannot be empty or blank";

  @Inject
  public CreateUserDataFetcher(UserService userService) {
    super(QLCreateUserInput.class, QLCreateUserPayload.class);
    this.userService = userService;
  }

  private QLUser prepareQLUser(User user) {
    return UserController.populateUser(user, QLUser.builder());
  }

  private QLCreateUserPayload prepareQLCreateUserPayload(QLUser user, String requestId) {
    return QLCreateUserPayload.builder().user(user).clientMutationId(requestId).build();
  }

  @Override
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLCreateUserPayload mutateAndFetch(QLCreateUserInput qlCreateUserInput, MutationContext mutationContext) {
    validate(qlCreateUserInput, mutationContext.getAccountId());
    String accountId = mutationContext.getAccountId();
    inviteUser(qlCreateUserInput, accountId);
    List<String> userGroupIds = new LinkedList<>();
    final RequestField<List<String>> userGroupIdsFromInput = qlCreateUserInput.getUserGroupIds();
    if (userGroupIdsFromInput != null && userGroupIdsFromInput.isPresent()) {
      userGroupIds = getValue(qlCreateUserInput.getUserGroupIds()).orElse(Collections.emptyList());
    }
    final User savedUser = userService.getUserByEmail(qlCreateUserInput.getEmail(), accountId);
    userGroupController.addUserToUserGroups(savedUser, userGroupIds, accountId);

    return prepareQLCreateUserPayload(prepareQLUser(savedUser), qlCreateUserInput.getClientMutationId());
  }

  private void inviteUser(QLCreateUserInput qlCreateUserInput, final String accountId) {
    UserInvite userInvite = new UserInvite();
    userInvite.setAccountId(accountId);
    userInvite.setName(qlCreateUserInput.getName());
    userInvite.setSource(UserInviteSource.builder().type(UserInviteSource.SourceType.MANUAL).uuid("").build());
    userInvite.setEmails(Arrays.asList(qlCreateUserInput.getEmail()));
    userInvite.setAppId(GLOBAL_APP_ID);
    userService.inviteUsers(userInvite);
  }

  private void validate(QLCreateUserInput qlCreateUserInput, final String accountId) {
    if (StringUtils.isBlank(qlCreateUserInput.getEmail())) {
      throw new InvalidArgumentsException(Pair.of("email", INVALID_VAL_INP_PARAM_ERR_MSSG));
    }
    if (StringUtils.isBlank(qlCreateUserInput.getName())) {
      throw new InvalidArgumentsException(Pair.of("name", INVALID_VAL_INP_PARAM_ERR_MSSG));
    }
    final RequestField<List<String>> userGroupIds = qlCreateUserInput.getUserGroupIds();
    if (isInitialized(userGroupIds)) {
      final List<String> userGroupIdList = getValue(userGroupIds).orElse(Collections.emptyList());
      if (userGroupIdList.contains("")) {
        throw new InvalidArgumentsException(Pair.of("userGroupId", INVALID_VAL_INP_PARAM_ERR_MSSG));
      }
      userGroupController.checkIfUserGroupIdsExist(accountId, userGroupIdList);
    }
  }

  private boolean isInitialized(RequestField<?> field) {
    return field != null && field.isPresent();
  }

  private <T> Optional<T> getValue(RequestField<T> obj) {
    return obj.getValue();
  }
}