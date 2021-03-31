package software.wings.resources;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.remote.UserSearchFilter;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.beans.User;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api(value = "/ng/user", hidden = true)
@Path("/ng/user")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class UserResourceNG {
  private final UserService userService;
  private static final String ACCOUNT_ADMINISTRATOR_USER_GROUP = "Account Administrator";

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

  @POST
  @Path("/batch")
  public RestResponse<List<UserInfo>> listUsersByIds(
      @QueryParam("accountId") String accountId, UserSearchFilter userSearchFilter) {
    List<User> users = new ArrayList<>();
    if (!isEmpty(userSearchFilter.getUserIds())) {
      users.addAll(userService.getUsers(userSearchFilter.getUserIds(), accountId));
    }
    if (!isEmpty(userSearchFilter.getEmailIds())) {
      users.addAll(userService.getUsersByEmail(userSearchFilter.getEmailIds(), accountId));
    }
    return new RestResponse<>(convertUserToNgUser(users));
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

  private List<UserInfo> convertUserToNgUser(List<User> userList) {
    return userList.stream()
        .map(user -> UserInfo.builder().email(user.getEmail()).name(user.getName()).uuid(user.getUuid()).build())
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
        .admin(
            Optional.ofNullable(user.getUserGroups())
                .map(x
                    -> x.stream().anyMatch(y -> ACCOUNT_ADMINISTRATOR_USER_GROUP.equals(y.getName()) && y.isDefault()))
                .orElse(false))
        .build();
  }
}
