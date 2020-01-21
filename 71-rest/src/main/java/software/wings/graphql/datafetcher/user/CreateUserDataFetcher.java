package software.wings.graphql.datafetcher.user;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
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
    return QLCreateUserPayload.builder().user(user).requestId(requestId).build();
  }

  @Override
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  protected QLCreateUserPayload mutateAndFetch(QLCreateUserInput qlCreateUserInput, MutationContext mutationContext) {
    validate(qlCreateUserInput);
    String accountId = mutationContext.getAccountId();
    inviteUser(qlCreateUserInput, accountId);
    final User savedUser = userService.getUserByEmail(qlCreateUserInput.getEmail(), accountId);
    userGroupController.addUserToUserGroups(savedUser, qlCreateUserInput.getUserGroupIds(), accountId);

    return prepareQLCreateUserPayload(prepareQLUser(savedUser), qlCreateUserInput.getRequestId());
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

  private void validate(QLCreateUserInput qlCreateUserInput) {
    if (StringUtils.isBlank(qlCreateUserInput.getEmail())) {
      throw new InvalidArgumentsException(Pair.of("email", INVALID_VAL_INP_PARAM_ERR_MSSG));
    }
    if (StringUtils.isBlank(qlCreateUserInput.getName())) {
      throw new InvalidArgumentsException(Pair.of("name", INVALID_VAL_INP_PARAM_ERR_MSSG));
    }
    if (StringUtils.isBlank(qlCreateUserInput.getRequestId())) {
      throw new InvalidArgumentsException(Pair.of("requestId", INVALID_VAL_INP_PARAM_ERR_MSSG));
    }
  }
}