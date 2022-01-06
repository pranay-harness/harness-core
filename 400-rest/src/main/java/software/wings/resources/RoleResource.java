/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;

import software.wings.beans.Role;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.RoleService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
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
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by anubhaw on 3/22/16.
 */
@Api("roles")
@Path("/roles")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.ROLE)
public class RoleResource {
  @Inject private RoleService roleService;

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Role>> list(
      @BeanParam PageRequest<Role> pageRequest, @QueryParam("accountId") @NotEmpty String accountId) {
    pageRequest.setLimit(PageRequest.UNLIMITED);
    return new RestResponse<>(roleService.list(pageRequest));
  }

  /**
   * Save.
   *
   * @param role the role
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
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
  @Timed
  @ExceptionMetered
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
  @Timed
  @ExceptionMetered
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
  @Timed
  @ExceptionMetered
  public RestResponse<Role> get(@PathParam("roleId") String roleId) {
    return new RestResponse<>(roleService.get(roleId));
  }
}
