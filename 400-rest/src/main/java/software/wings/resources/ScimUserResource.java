/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.scim.PatchRequest;
import io.harness.scim.ScimListResponse;
import io.harness.scim.ScimResource;
import io.harness.scim.ScimUser;
import io.harness.security.annotations.ScimAPI;

import software.wings.scim.ScimUserServiceImpl;

import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.extern.slf4j.Slf4j;

@Api("scim")
@Path("/scim/account/{accountId}/")
@Consumes({"application/scim+json", "application/json"})
@Produces("application/scim+json")
@Slf4j
@ScimAPI
public class ScimUserResource extends ScimResource {
  @Inject private ScimUserServiceImpl scimUserServiceImpl;

  @POST
  @Path("Users")
  @ApiOperation(value = "Create a new user")
  public Response createUser(ScimUser userQuery, @PathParam("accountId") String accountId) {
    try {
      return scimUserServiceImpl.createUser(userQuery, accountId);
    } catch (Exception ex) {
      log.error("Failed to create the user", ex);
      return getExceptionResponse(ex, Status.NOT_FOUND.getStatusCode(), Status.CONFLICT);
    }
  }

  @PUT
  @Path("Users/{userId}")
  @ApiOperation(value = "Update an existing user by uuid")
  public Response updateUser(
      @PathParam("userId") String userId, @PathParam("accountId") String accountId, ScimUser userQuery) {
    try {
      return scimUserServiceImpl.updateUser(userId, accountId, userQuery);
    } catch (Exception ex) {
      log.info("Failed to update the user with id: {} for account: {}", userId, accountId, ex);
      return getExceptionResponse(ex, Status.NOT_FOUND.getStatusCode(), Status.NOT_FOUND);
    }
  }

  @GET
  @Path("Users/{userId}")
  @ApiOperation(value = "Get an existing user by uuid")
  public Response getUser(@PathParam("userId") String userId, @PathParam("accountId") String accountId) {
    try {
      return Response.status(Response.Status.OK).entity(scimUserServiceImpl.getUser(userId, accountId)).build();
    } catch (Exception ex) {
      log.info("Failed to fetch the user with id: {} for account: {}", userId, accountId, ex);
      return getExceptionResponse(ex, Status.NOT_FOUND.getStatusCode(), Status.NOT_FOUND);
    }
  }

  @GET
  @Path("Users")
  @ApiOperation(
      value =
          "Search users by their email address. Supports pagination. If nothing is passed in filter, all results will be returned.")
  public Response
  searchUser(@PathParam("accountId") String accountId, @QueryParam("filter") String filter,
      @QueryParam("count") Integer count, @QueryParam("startIndex") Integer startIndex) {
    try {
      ScimListResponse<ScimUser> searchUserResponse =
          scimUserServiceImpl.searchUser(accountId, filter, count, startIndex);
      return Response.status(Response.Status.OK).entity(searchUserResponse).build();
    } catch (Exception ex) {
      log.error("SCIM: Search user call failed. AccountId: {}, filter: {}, count: {}, startIndex{}", accountId, filter,
          count, startIndex, ex);
      return getExceptionResponse(ex, Status.NOT_FOUND.getStatusCode(), Status.NOT_FOUND);
    }
  }

  @DELETE
  @Path("Users/{userId}")
  @ApiOperation(value = "Delete an user by uuid")
  public Response deleteUser(@PathParam("userId") String userId, @PathParam("accountId") String accountId) {
    scimUserServiceImpl.deleteUser(userId, accountId);
    return javax.ws.rs.core.Response.status(Status.NO_CONTENT).build();
  }

  @PATCH
  @Path("Users/{userId}")
  @ApiOperation(value = "Update some fields of a user by uuid")
  public ScimUser updateUser(
      @PathParam("accountId") String accountId, @PathParam("userId") String userId, PatchRequest patchRequest) {
    return scimUserServiceImpl.updateUser(accountId, userId, patchRequest);
  }
}
