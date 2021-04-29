package software.wings.resources;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.security.dto.PrincipalType.USER;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.mappers.AccountMapper;
import io.harness.ng.core.user.TwoFactorAdminOverrideSettings;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.rest.RestResponse;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.dto.UserPrincipal;
import io.harness.user.remote.UserFilterNG;

import software.wings.beans.User;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.security.authentication.TwoFactorAuthenticationMechanism;
import software.wings.security.authentication.TwoFactorAuthenticationSettings;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

@Api(value = "/ng/user", hidden = true)
@Path("/ng/user")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class UserResourceNG {
  private final UserService userService;
  private final TwoFactorAuthenticationManager twoFactorAuthenticationManager;
  private static final String ACCOUNT_ADMINISTRATOR_USER_GROUP = "Account Administrator";

  @POST
  public RestResponse<UserInfo> createNewUserAndSignIn(UserRequestDTO userRequest) {
    User user = convertUserRequesttoUser(userRequest);
    String accountId = user.getDefaultAccountId();

    User createdUser = userService.createNewUserAndSignIn(user, accountId);

    return new RestResponse<>(convertUserToNgUser(createdUser));
  }

  @POST
  @Path("/oauth")
  public RestResponse<UserInfo> createNewOAuthUserAndSignIn(UserRequestDTO userRequest) {
    User user = convertUserRequesttoUser(userRequest);
    String accountId = user.getDefaultAccountId();

    User createdUser = userService.createNewOAuthUser(user, accountId);

    return new RestResponse<>(convertUserToNgUser(createdUser));
  }

  @GET
  @Path("/search")
  public RestResponse<PageResponse<UserInfo>> list(@BeanParam PageRequest<User> pageRequest,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("searchTerm") String searchTerm) {
    Integer offset = Integer.valueOf(pageRequest.getOffset());
    Integer pageSize = pageRequest.getPageSize();

    List<User> userList = userService.listUsers(pageRequest, accountId, searchTerm, offset, pageSize, true);

    PageResponse<UserInfo> pageResponse =
        aPageResponse()
            .withOffset(offset.toString())
            .withLimit(pageSize.toString())
            .withResponse(userList.stream().map(this::convertUserToNgUser).collect(Collectors.toList()))
            .withTotal(userService.getTotalUserCount(accountId, true))
            .build();

    return new RestResponse<>(pageResponse);
  }

  @GET
  @Path("/{userId}")
  public RestResponse<Optional<UserInfo>> getUser(@PathParam("userId") String userId) {
    User user = userService.get(userId);
    return new RestResponse<>(Optional.ofNullable(convertUserToNgUser(user)));
  }

  @GET
  @Path("email/{emailId}")
  public RestResponse<Optional<UserInfo>> getUserByEmailId(@PathParam("emailId") String emailId) {
    User user = userService.getUserByEmail(emailId);
    return new RestResponse<>(Optional.ofNullable(convertUserToNgUser(user)));
  }

  @POST
  @Path("/batch")
  public RestResponse<List<UserInfo>> listUsers(@QueryParam("accountId") String accountId, UserFilterNG userFilterNG) {
    Set<User> userSet = new HashSet<>();
    if (!isEmpty(userFilterNG.getUserIds())) {
      userSet.addAll(userService.getUsers(userFilterNG.getUserIds(), accountId));
    }
    if (!isEmpty(userFilterNG.getEmailIds())) {
      userSet.addAll(userService.getUsersByEmail(userFilterNG.getEmailIds(), accountId));
    }
    return new RestResponse<>(convertUserToNgUser(new ArrayList<>(userSet)));
  }

  @PUT
  @Path("/user")
  public RestResponse<Optional<UserInfo>> updateUser(@Body UserInfo userInfo) {
    User user = convertNgUserToUserWithNameUpdated(userInfo);
    user = userService.update(user);
    return new RestResponse<>(Optional.ofNullable(convertUserToNgUser(user)));
  }

  @GET
  @Path("/user-account")
  public RestResponse<Boolean> isUserInAccount(
      @NotNull @QueryParam("accountId") String accountId, @QueryParam("userId") String userId) {
    try {
      User user = userService.getUserFromCacheOrDB(userId);
      boolean isUserInAccount = false;
      if (user != null && user.getAccounts() != null) {
        isUserInAccount = user.getAccounts().stream().anyMatch(account -> account.getUuid().equals(accountId));
      }
      if (!isUserInAccount && user != null && user.getSupportAccounts() != null) {
        isUserInAccount = user.getSupportAccounts().stream().anyMatch(account -> account.getUuid().equals(accountId));
      }
      if (!isUserInAccount) {
        log.error(String.format("User %s does not belong to account %s", userId, accountId));
      }
      return new RestResponse<>(isUserInAccount);
    } catch (Exception ex) {
      log.error(String.format("User %s does not belong to account %s", userId, accountId), ex);
      return new RestResponse<>(false);
    }
  }

  @POST
  @Path("/user-account")
  public RestResponse<Boolean> addUserToAccount(
      @QueryParam("userId") String userId, @QueryParam("accountId") String accountId) {
    userService.addUserToAccount(userId, accountId);
    return new RestResponse<>(true);
  }

  @DELETE
  @Path("/safeDelete/{userId}")
  public RestResponse<Boolean> safeDeleteUser(
      @PathParam("userId") String userId, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(userService.safeDeleteUser(userId, accountId));
  }

  @GET
  @Path("/two-factor-auth/{auth-mechanism}")
  public RestResponse<Optional<TwoFactorAuthenticationSettings>> getTwoFactorAuthSettings(
      @PathParam("auth-mechanism") TwoFactorAuthenticationMechanism authMechanism,
      @QueryParam("emailId") String emailId) {
    return new RestResponse<>(Optional.ofNullable(twoFactorAuthenticationManager.createTwoFactorAuthenticationSettings(
        userService.getUserByEmail(emailId), authMechanism)));
  }

  @PUT
  @Path("/enable-two-factor-auth")
  public RestResponse<Optional<UserInfo>> enableTwoFactorAuth(
      @QueryParam("emailId") String emailId, @Body TwoFactorAuthenticationSettings settings) {
    return new RestResponse<>(
        Optional.ofNullable(convertUserToNgUser(twoFactorAuthenticationManager.enableTwoFactorAuthenticationSettings(
            userService.getUserByEmail(emailId), settings))));
  }

  @PUT
  @Path("/disable-two-factor-auth")
  public RestResponse<Optional<UserInfo>> disableTwoFactorAuth(@QueryParam("emailId") String emailId) {
    return new RestResponse<>(Optional.ofNullable(convertUserToNgUser(
        twoFactorAuthenticationManager.disableTwoFactorAuthentication(userService.getUserByEmail(emailId)))));
  }

  private List<UserInfo> convertUserToNgUser(List<User> userList) {
    return userList.stream()
        .map(user
            -> UserInfo.builder()
                   .email(user.getEmail())
                   .name(user.getName())
                   .uuid(user.getUuid())
                   .admin(Optional.ofNullable(user.getUserGroups())
                              .map(x
                                  -> x.stream().anyMatch(
                                      y -> ACCOUNT_ADMINISTRATOR_USER_GROUP.equals(y.getName()) && y.isDefault()))
                              .orElse(false))
                   .twoFactorAuthenticationEnabled(user.isTwoFactorAuthenticationEnabled())
                   .build())
        .collect(Collectors.toList());
  }

  private UserInfo convertUserToNgUser(User user) {
    if (user == null) {
      return null;
    }
    return UserInfo.builder()
        .email(user.getEmail())
        .name(user.getName())
        .uuid(user.getUuid())
        .defaultAccountId(user.getDefaultAccountId())
        .twoFactorAuthenticationEnabled(user.isTwoFactorAuthenticationEnabled())
        .token(user.getToken())

        .build();
  }

  private User convertUserRequesttoUser(UserRequestDTO userRequest) {
    if (userRequest == null) {
      return null;
    }

    return User.Builder.anUser()
        .email(userRequest.getEmail())
        .name(userRequest.getName())
        .twoFactorAuthenticationEnabled(userRequest.isTwoFactorAuthenticationEnabled())
        .passwordHash(userRequest.getPasswordHash())
        .accountName(userRequest.getAccountName())
        .companyName(userRequest.getCompanyName())
        .accounts(userRequest.getAccounts()
                      .stream()
                      .map(account -> AccountMapper.fromAccountDTO(account))
                      .collect(Collectors.toList()))
        .emailVerified(userRequest.isEmailVerified())
        .defaultAccountId(userRequest.getDefaultAccountId())
        .build();
  }

  private User convertNgUserToUserWithNameUpdated(UserInfo userInfo) {
    if (userInfo == null) {
      return null;
    }
    User user = userService.getUserByEmail(userInfo.getEmail());
    user.setName(userInfo.getName());
    return user;
  }

  @PUT
  @Path("two-factor-admin-override-settings")
  public RestResponse<Boolean> setTwoFactorAuthAtAccountLevel(
      @QueryParam("accountId") @NotEmpty String accountId, @NotNull TwoFactorAdminOverrideSettings settings) {
    // Trying Override = true
    if (settings.isAdminOverrideTwoFactorEnabled()) {
      if (SourcePrincipalContextBuilder.getSourcePrincipal() == null
          || !USER.equals(SourcePrincipalContextBuilder.getSourcePrincipal().getType())) {
        throw new InvalidRequestException("Unable to fetch current user");
      }

      UserPrincipal userPrincipal = (UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
      User user = userService.getUserByEmail(userPrincipal.getEmail());

      if (twoFactorAuthenticationManager.isTwoFactorEnabled(accountId, user)) {
        return new RestResponse(twoFactorAuthenticationManager.overrideTwoFactorAuthentication(accountId, settings));
      } else {
        throw new InvalidRequestException("Admin has 2FA disabled. Please enable to enforce 2FA on users.");
      }
    }
    // Trying Override = false
    else {
      return new RestResponse(twoFactorAuthenticationManager.overrideTwoFactorAuthentication(accountId, settings));
    }
  }
}
