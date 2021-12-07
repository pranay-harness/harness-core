package io.harness.ng.scim.resource;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.scim.NGScimUserService;
import io.harness.security.annotations.ScimAPI;

import software.wings.beans.scim.ScimUser;
import software.wings.scim.PatchRequest;

import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import lombok.extern.slf4j.Slf4j;

@Api("scim")
@Path("/scim/account/{accountIdentifier}")
@Consumes({"application/scim+json", "application/json"})
@Produces("application/scim+json")
@Slf4j
@ScimAPI
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Tag(name = "SCIM", description = "This contains APIs related to SCIM provisioning")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
public class NGScimUserResource {
  @Inject private NGScimUserService scimUserService;

  @POST
  @Path("Users")
  @ApiOperation(value = "Create a new user", nickname = "createScimUser")
  public Response createUser(ScimUser userQuery, @PathParam("accountIdentifier") String accountIdentifier) {
    return null;
  }

  @PUT
  @Path("Users/{userIdentifier}")
  @ApiOperation(value = "Update an existing user by uuid", nickname = "updateScimUser")
  public Response updateUser(@PathParam("userIdentifier") String userIdentifier,
      @PathParam("accountIdentifier") String accountIdentifier, ScimUser userQuery) {
    return null;
  }

  @GET
  @Path("Users/{userIdentifier}")
  @ApiOperation(value = "Get an existing user by uuid", nickname = "getScimUser")
  public Response getUser(
      @PathParam("userIdentifier") String userIdentifier, @PathParam("accountIdentifier") String accountIdentifier) {
    return null;
  }

  @GET
  @Path("Users")
  @ApiOperation(
      value =
          "Search users by their email address. Supports pagination. If nothing is passed in filter, all results will be returned.",
      nickname = "searchScimUser")
  public Response
  searchUser(@PathParam("accountIdentifier") String accountIdentifier, @QueryParam("filter") String filter,
      @QueryParam("count") Integer count, @QueryParam("startIndex") Integer startIndex) {
    return null;
  }

  @DELETE
  @Path("Users/{userIdentifier}")
  @ApiOperation(value = "Delete an user by uuid", nickname = "deleteScimUser")
  public Response deleteUser(
      @PathParam("userIdentifier") String userIdentifier, @PathParam("accountIdentifier") String accountIdentifier) {
    return null;
  }

  @PATCH
  @Path("Users/{userIdentifier}")
  @ApiOperation(value = "Update some fields of a user by uuid", nickname = "patchScimUser")
  public ScimUser updateUser(@PathParam("accountIdentifier") String accountIdentifier,
      @PathParam("userIdentifier") String userIdentifier, PatchRequest patchRequest) {
    return null;
  }
}
