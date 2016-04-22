package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.security.annotations.AuthRule;
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
  @Path("{roleId}")
  public void delete(@PathParam("{roleId}") String roleId) {
    roleService.delete(roleId);
  }

  @GET
  @Path("{roleId}")
  public RestResponse<Role> get(@PathParam("roleId") String roleId) {
    return new RestResponse<>(roleService.findByUuid(roleId));
  }
}
