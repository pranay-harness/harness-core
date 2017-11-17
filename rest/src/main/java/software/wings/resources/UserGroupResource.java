package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.UserGroup;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.UserGroupService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * Users Resource class.
 *
 * @author Rishi
 */
@Api("userGroups")
@Path("/userGroups")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AuthRule(ResourceType.USER)
public class UserGroupResource {
  private UserGroupService userGroupService;

  /**
   * Instantiates a new User resource.
   *
   * @param userGroupService    the userGroupService
   */
  @Inject
  public UserGroupResource(UserGroupService userGroupService) {
    this.userGroupService = userGroupService;
  }

  /**
   * List.
   *
   * @param pageRequest the page request
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<UserGroup>> list(
      @BeanParam PageRequest<UserGroup> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    pageRequest.addFilter("accountId", accountId, Operator.EQ);
    PageResponse<UserGroup> pageResponse = userGroupService.list(pageRequest);
    return new RestResponse<>(pageResponse);
  }

  /**
   * Gets the.
   *
   * @param accountId   the account id
   * @param userGroupId  the userGroupId
   * @return the rest response
   */
  @GET
  @Path("{userGroupId}")
  @Timed
  @ExceptionMetered
  public RestResponse<UserGroup> get(
      @QueryParam("accountId") String accountId, @PathParam("userGroupId") String userGroupId) {
    return new RestResponse<>(userGroupService.get(accountId, userGroupId));
  }

  /**
   * Save.
   *
   * @param accountId   the account id
   * @param userGroup the userGroup
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<UserGroup> save(@QueryParam("accountId") String accountId, UserGroup userGroup) {
    userGroup.setAccountId(accountId);
    return new RestResponse<>(userGroupService.save(userGroup));
  }

  /**
   * Update Overview.
   *
   * @param accountId   the account id
   * @param userGroupId  the userGroupId
   * @param userGroup the userGroup
   * @return the rest response
   */
  @PUT
  @Path("{userGroupId}/overview")
  @Timed
  @ExceptionMetered
  public RestResponse<UserGroup> updateOverview(
      @QueryParam("accountId") String accountId, @PathParam("userGroupId") String userGroupId, UserGroup userGroup) {
    userGroup.setAccountId(accountId);
    return new RestResponse<>(userGroupService.updateOverview(userGroup));
  }

  /**
   * Update Members.
   *
   * @param accountId   the account id
   * @param userGroupId  the userGroupId
   * @param userGroup the userGroup
   * @return the rest response
   */
  @PUT
  @Path("{userGroupId}/members")
  @Timed
  @ExceptionMetered
  public RestResponse<UserGroup> updateMembers(
      @QueryParam("accountId") String accountId, @PathParam("userGroupId") String userGroupId, UserGroup userGroup) {
    userGroup.setAccountId(accountId);
    return new RestResponse<>(userGroupService.updateMembers(userGroup));
  }

  /**
   * Update Permission.
   *
   * @param accountId   the account id
   * @param userGroupId  the userGroupId
   * @param userGroup the userGroup
   * @return the rest response
   */
  @PUT
  @Path("{userGroupId}/permissions")
  @Timed
  @ExceptionMetered
  public RestResponse<UserGroup> updatePermissions(
      @QueryParam("accountId") String accountId, @PathParam("userGroupId") String userGroupId, UserGroup userGroup) {
    userGroup.setAccountId(accountId);
    return new RestResponse<>(userGroupService.updatePermissions(userGroup));
  }
}
