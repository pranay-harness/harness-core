package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.RoleService;

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
 * Created by anubhaw on 3/22/16.
 */
@Api("roles")
@Path("/roles")
@Timed
@ExceptionMetered
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoleResource {
  @Inject private RoleService roleService;

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Role>> list(
      @BeanParam PageRequest<Role> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(roleService.list(pageRequest));
  }

  /**
   * Save.
   *
   * @param role the role
   * @return the rest response
   */
  @POST
  public RestResponse<Role> save(Role role) {
    return new RestResponse<>(roleService.save(role));
  }

  /**
   * Update.
   *
   * @param roleId the role id
   * @param role   the role
   * @return the rest response
   */
  @PUT
  @Path("{roleId}")
  public RestResponse<Role> update(@PathParam("roleId") String roleId, Role role) {
    role.setUuid(roleId);
    return new RestResponse<>(roleService.update(role));
  }

  /**
   * Delete.
   *
   * @param roleId the role id
   * @return the rest response
   */
  @DELETE
  @Path("{roleId}")
  public RestResponse delete(@PathParam("{roleId}") String roleId) {
    roleService.delete(roleId);
    return new RestResponse();
  }

  /**
   * Gets the.
   *
   * @param roleId the role id
   * @return the rest response
   */
  @GET
  @Path("{roleId}")
  public RestResponse<Role> get(@PathParam("roleId") String roleId) {
    return new RestResponse<>(roleService.get(roleId));
  }
}
