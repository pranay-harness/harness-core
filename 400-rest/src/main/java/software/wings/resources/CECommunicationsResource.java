/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.CE_ADMIN;
import static software.wings.security.PermissionAttribute.ResourceType.USER;

import io.harness.ccm.communication.CECommunicationsService;
import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CommunicationType;
import io.harness.exception.InvalidRequestException;
import io.harness.rest.RestResponse;

import software.wings.beans.User;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("ceCommunications")
@Path("/ceCommunications")
@Produces("application/json")
@Scope(USER)
public class CECommunicationsResource {
  private final CECommunicationsService communicationsService;

  @Inject
  public CECommunicationsResource(CECommunicationsService communicationsService) {
    this.communicationsService = communicationsService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<CECommunications>> get(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(communicationsService.list(accountId, getUserEmail()));
  }

  @POST
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = CE_ADMIN)
  public RestResponse update(@QueryParam("accountId") String accountId, @QueryParam("type") CommunicationType type,
      @QueryParam("enable") boolean enable) {
    communicationsService.update(accountId, getUserEmail(), type, enable, true);
    return new RestResponse<>();
  }

  @GET
  @Path("{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse getEntriesEnabledViaEmails(@PathParam("accountId") String accountId) {
    return new RestResponse<>(communicationsService.getEntriesEnabledViaEmail(accountId));
  }

  @POST
  @Path("{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse enableViaEmail(@PathParam("accountId") String accountId,
      @QueryParam("type") CommunicationType type, @QueryParam("email") String email,
      @QueryParam("enable") boolean enable) {
    communicationsService.update(accountId, email, type, enable, false);
    return new RestResponse<>();
  }

  @DELETE
  @Path("{accountId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = CE_ADMIN)
  public RestResponse removeEmail(@PathParam("accountId") String accountId, @QueryParam("type") CommunicationType type,
      @QueryParam("email") String email) {
    communicationsService.delete(accountId, email, type);
    return new RestResponse<>();
  }

  @POST
  @Path("{accountId}/addEmails")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = CE_ADMIN)
  public RestResponse addMultipleEmail(
      @PathParam("accountId") String accountId, @QueryParam("type") CommunicationType type, List<String> emails) {
    emails.forEach(email -> communicationsService.update(accountId, email, type, true, false));
    return new RestResponse<>();
  }

  @POST
  @Path("{accountId}/internal")
  @Timed
  @ExceptionMetered
  public RestResponse enableViaEmailInternal(@PathParam("accountId") String accountId,
      @QueryParam("targetAccount") String targetAccount, @QueryParam("type") CommunicationType type,
      @QueryParam("email") String email) {
    communicationsService.update(targetAccount, email, type, true, true);
    return new RestResponse();
  }

  @DELETE
  @Path("{accountId}/internal")
  @Timed
  @ExceptionMetered
  public RestResponse removeEmailInternal(@PathParam("accountId") String accountId,
      @QueryParam("targetAccount") String targetAccount, @QueryParam("type") CommunicationType type,
      @QueryParam("email") String email) {
    communicationsService.delete(targetAccount, email, type);
    return new RestResponse();
  }

  private String getUserEmail() {
    User existingUser = UserThreadLocal.get();
    if (existingUser == null) {
      throw new InvalidRequestException("Invalid User");
    }
    return existingUser.getEmail();
  }
}
