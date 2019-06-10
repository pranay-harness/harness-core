package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.beans.SearchFilter.Operator;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.HarnessApiKey.ClientType;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.security.annotations.HarnessApiKeyAuth;
import software.wings.security.annotations.IdentityServiceAuth;
import software.wings.security.authentication.AuthenticationManager;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.UserService;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author mark.lu on 2019-04-29
 */
@Api("identity")
@Path("/identity")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@HarnessApiKeyAuth(clientTypes = ClientType.IDENTITY_SERVICE)
@Slf4j
public class IdentityServiceResource {
  private AuthenticationManager authenticationManager;
  private UserService userService;
  private AccountService accountService;
  private HarnessUserGroupService harnessUserGroupService;

  @Inject
  public IdentityServiceResource(AuthenticationManager authenticationManager, UserService userService,
      AccountService accountService, HarnessUserGroupService harnessUserGroupService) {
    this.authenticationManager = authenticationManager;
    this.userService = userService;
    this.accountService = accountService;
    this.harnessUserGroupService = harnessUserGroupService;
  }

  @GET
  @Path("/user/login")
  @Timed
  @ExceptionMetered
  @IdentityServiceAuth
  public RestResponse<User> loginUser(@QueryParam("email") String email) {
    return new RestResponse<>(authenticationManager.loginUserForIdentityService(urlDecode(email)));
  }

  private String urlDecode(String encoded) {
    String decoded = encoded;
    try {
      decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      // Should not happen and ignore.
    }
    return decoded;
  }

  @GET
  @Path("/users")
  @Timed
  @ExceptionMetered
  @IdentityServiceAuth
  public RestResponse<List<User>> listUsers(@BeanParam PageRequest<User> pageRequest) {
    // Filter out 'disabled' users
    pageRequest.getFilters().add(SearchFilter.builder()
                                     .fieldName(UserKeys.disabled)
                                     .op(Operator.EQ)
                                     .fieldValues(new Object[] {Boolean.FALSE})
                                     .build());
    PageResponse<User> pageResponse = userService.list(pageRequest, false);
    return new RestResponse<>(pageResponse.getResponse());
  }

  @GET
  @Path("/accounts")
  @Timed
  @ExceptionMetered
  @IdentityServiceAuth
  public RestResponse<List<Account>> listAccounts(@BeanParam PageRequest<Account> pageRequest) {
    return new RestResponse<>(accountService.list(pageRequest));
  }

  @GET
  @Path("/harnessUserGroups")
  @Timed
  @ExceptionMetered
  @IdentityServiceAuth
  public RestResponse<List<HarnessUserGroup>> getHarnessUserGroup(
      @BeanParam PageRequest<HarnessUserGroup> pageRequest) {
    return new RestResponse<>(harnessUserGroupService.list(pageRequest).getResponse());
  }
}
