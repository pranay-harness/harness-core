package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.security.UserGroup;
import software.wings.dl.PageRequest;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.UserGroupService;

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
@Scope(ResourceType.USER)
@AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
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
  @AuthRule(permissionType = PermissionType.USER_PERMISSION_MANAGEMENT)
  public RestResponse<PageResponse<UserGroup>> list(
      @BeanParam PageRequest<UserGroup> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
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
   * Clone the user group with the given id and a new name
   * @param accountId The account it
   * @param userGroupId The user group id to clone
   * @param newName The name to be set of the new cloned user group
   * @return The rest response.
   */
  @POST
  @Path("{userGroupId}/clone")
  @Timed
  @ExceptionMetered
  public RestResponse<UserGroup> clone(@QueryParam("accountId") String accountId,
      @PathParam("userGroupId") String userGroupId, @QueryParam("newName") String newName,
      @QueryParam("newDescription") String newDescription) {
    return new RestResponse<>(userGroupService.cloneUserGroup(accountId, userGroupId, newName, newDescription));
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
    userGroup.setUuid(userGroupId);
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
    userGroup.setUuid(userGroupId);
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
    userGroup.setUuid(userGroupId);
    userGroup.setAccountId(accountId);
    return new RestResponse<>(userGroupService.updatePermissions(userGroup));
  }

  /**
   * Delete.
   *
   * @param accountId   the account id
   * @param userGroupId  the userGroupId
   * @return the rest response
   */
  @DELETE
  @Path("{userGroupId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> delete(
      @QueryParam("accountId") String accountId, @PathParam("userGroupId") String userGroupId) {
    return new RestResponse<>(userGroupService.delete(accountId, userGroupId));
  }

  /**
   * List for approvals
   *
   * @param accountId   the account id
   * @return the rest response
   */
  @GET
  @Path("approvals")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<PageResponse<UserGroup>> listForApprovals(@QueryParam("accountId") @NotEmpty String accountId) {
    PageRequest<UserGroup> pageRequest = PageRequestBuilder.aPageRequest()
                                             .addFilter("accountId", Operator.EQ, accountId)
                                             .addFieldsIncluded("_id", "name")
                                             .build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, false);
    return new RestResponse<>(pageResponse);
  }
}
