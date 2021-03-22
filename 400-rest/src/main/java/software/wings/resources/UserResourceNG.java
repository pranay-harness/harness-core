package software.wings.resources;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.beans.User;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
public class UserResourceNG {
  private final UserService userService;

  @GET
  @Path("/search")
  public RestResponse<PageResponse<User>> list(@BeanParam PageRequest<User> pageRequest,
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("searchTerm") String searchTerm) {
    Integer offset = Integer.valueOf(pageRequest.getOffset());
    Integer pageSize = pageRequest.getPageSize();

    List<User> userList = userService.listUsers(pageRequest, accountId, searchTerm, offset, pageSize, false);

    PageResponse<User> pageResponse = aPageResponse()
                                          .withOffset(offset.toString())
                                          .withLimit(pageSize.toString())
                                          .withResponse(userList)
                                          .withTotal(userService.getTotalUserCount(accountId, true))
                                          .build();

    return new RestResponse<>(pageResponse);
  }

  @GET
  public RestResponse<PageResponse<User>> listUsersInAccount(
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("searchTerm") String searchTerm) {
    Integer offset = 0;
    Integer pageSize = 100000;

    List<User> userList = userService.listUsers(new PageRequest<User>(), accountId, searchTerm, offset, pageSize, true);

    PageResponse<User> pageResponse =
        aPageResponse().withOffset(offset.toString()).withLimit(pageSize.toString()).withResponse(userList).build();

    return new RestResponse<>(pageResponse);
  }

  @POST
  @Path("/batch")
  public RestResponse<List<User>> listUsersByIds(List<String> userIds) {
    return new RestResponse<>(userService.getUsers(userIds));
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
  public RestResponse<Optional<User>> getUserFromEmail(
      @QueryParam("accountId") String accountId, @QueryParam("emailId") String emailId) {
    return new RestResponse<>(Optional.ofNullable(userService.getUserByEmail(emailId, accountId)));
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
}
