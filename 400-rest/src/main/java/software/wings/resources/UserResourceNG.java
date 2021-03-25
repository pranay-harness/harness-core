package software.wings.resources;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.ng.core.user.UserInfo;
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
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api(value = "/ng/users", hidden = true)
@Path("/ng/users")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class UserResourceNG {
  private final UserService userService;

  @GET
  @Path("/search")
  public RestResponse<PageResponse<UserInfo>> list(@BeanParam PageRequest<User> pageRequest,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("searchTerm") String searchTerm,
      @QueryParam("loadUserGroups") @DefaultValue("false") boolean loadUserGroups) {
    Integer offset = Integer.valueOf(pageRequest.getOffset());
    Integer pageSize = pageRequest.getPageSize();

    List<User> userList = userService.listUsers(pageRequest, accountId, searchTerm, offset, pageSize, loadUserGroups);

    PageResponse<UserInfo> pageResponse =
        aPageResponse()
            .withOffset(offset.toString())
            .withLimit(pageSize.toString())
            .withResponse(userList.stream().map(this::convertUserToNgUser).collect(Collectors.toList()))
            .withTotal(userService.getTotalUserCount(accountId, true))
            .build();

    return new RestResponse<>(pageResponse);
  }

  @POST
  @Path("/batch")
  public RestResponse<List<UserInfo>> listUsersByIds(List<String> userIds) {
    return new RestResponse<>(convertUserToNgUser(userService.getUsers(userIds)));
  }

  @GET
  @Path("/usernames")
  public RestResponse<List<String>> getUsernameFromEmail(
      @QueryParam("accountId") String accountId, @QueryParam("emailList") List<String> emailList) {
    List<String> usernames = new ArrayList<>();
    for (String email : emailList) {
      Optional<User> user = Optional.ofNullable(userService.getUserByEmail(email, accountId));
      if (user.isPresent()) {
        usernames.add(user.get().getName());
      } else {
        usernames.add(null);
      }
    }
    return new RestResponse<>(usernames);
  }

  @GET
  public RestResponse<Optional<UserInfo>> getUserFromEmail(@QueryParam("emailId") String emailId) {
    User user = userService.getUserByEmail(emailId);
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

  private List<UserInfo> convertUserToNgUser(List<User> userList) {
    return userList.stream()
        .map(user
            -> UserInfo.builder()
                   .email(user.getEmail())
                   .name(user.getName())
                   .uuid(user.getUuid())
                   .accountIds(user.getAccountIds())
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
        .accountIds(user.getAccountIds())
        .build();
  }
}
