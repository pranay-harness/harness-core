package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.RoleService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Created by anubhaw on 3/22/16.
 */

@Path("/roles")
@AuthRule
@Timed
@ExceptionMetered
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RoleResource {
  @Inject private RoleService roleService;

  @GET
  public RestResponse<PageResponse<Role>> list(@BeanParam PageRequest<Role> pageRequest) {
    return new RestResponse<>(roleService.list(pageRequest));
  }

  @POST
  public RestResponse<Role> save(Role role) {
    return new RestResponse<>(roleService.save(role));
  }

  @PUT
  public RestResponse<Role> update(Role role) {
    return new RestResponse<>(roleService.update(role));
  }

  @DELETE
  @Path("{roleID}")
  public void delete(@PathParam("{roleID}") String roleID) {
    roleService.delete(roleID);
  }

  @GET
  @Path("{roleID}")
  public RestResponse<Role> get(@PathParam("roleID") String roleID) {
    return new RestResponse<>(roleService.findByUUID(roleID));
  }
}
