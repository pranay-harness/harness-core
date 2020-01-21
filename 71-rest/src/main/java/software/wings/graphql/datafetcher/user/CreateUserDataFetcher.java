package software.wings.graphql.datafetcher.user;

import com.google.inject.Inject;

import io.harness.exception.InvalidArgumentsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.user.QLCreateUserInput;
import software.wings.graphql.schema.type.user.QLCreateUserPayload;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

@Slf4j
public class CreateUserDataFetcher extends BaseMutatorDataFetcher<QLCreateUserInput, QLCreateUserPayload> {
  @Inject private UserService userService;
  @Inject AccountService accountService;
  private final String INVALID_VAL_INP_PARAM_ERR_MSSG = "cannot be empty or blank";

  @Inject
  public CreateUserDataFetcher(UserService userService) {
    super(QLCreateUserInput.class, QLCreateUserPayload.class);
    this.userService = userService;
  }

  private User prepareUser(QLCreateUserInput createUserInput, final String accountId) {
    final Account account = accountService.get(accountId);
    return UserController.populateUser(User.Builder.anUser(), createUserInput, account).build();
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
    final User savedUser = userService.save(prepareUser(qlCreateUserInput, accountId), accountId);
    if (savedUser != null) {
      UserInvite userInvite = new UserInvite();
      userInvite.setAccountId(accountId);
      userInvite.setName(qlCreateUserInput.getName());
      userInvite.setEmail(qlCreateUserInput.getEmail());
      userService.inviteUsers(userInvite);
    }

    return prepareQLCreateUserPayload(prepareQLUser(savedUser), qlCreateUserInput.getRequestId());
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
