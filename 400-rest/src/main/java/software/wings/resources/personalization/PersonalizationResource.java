/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources.personalization;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT;

import io.harness.rest.RestResponse;

import software.wings.beans.User;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.personalization.PersonalizationService;
import software.wings.sm.StateType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("personalization")
@Path("/personalization")
@Produces("application/json")
@Scope(ResourceType.USER)
@AuthRule(permissionType = ACCOUNT)
public class PersonalizationResource {
  private PersonalizationService personalizationService;

  @Inject
  public PersonalizationResource(PersonalizationService PersonalizationService) {
    this.personalizationService = PersonalizationService;
  }

  @PUT
  @Path("steps/{stepId}/favorite")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public void addFavorite(@PathParam("stepId") String stepId, @QueryParam("accountId") String accountId) {
    final User user = UserThreadLocal.get();
    personalizationService.addFavoriteStep(StateType.valueOf(stepId), accountId, user.getUuid());
  }

  @DELETE
  @Path("steps/{stepId}/favorite")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public void removeFavorite(@PathParam("stepId") String stepId, @QueryParam("accountId") String accountId) {
    final User user = UserThreadLocal.get();
    personalizationService.removeFavoriteStep(StateType.valueOf(stepId), accountId, user.getUuid());
  }

  @PUT
  @Path("steps/{stepId}/recent")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public void addRecent(@PathParam("stepId") String stepId, @QueryParam("accountId") String accountId) {
    final User user = UserThreadLocal.get();
    personalizationService.addRecentStep(StateType.valueOf(stepId), accountId, user.getUuid());
  }

  @PUT
  @Path("templates/{templateId}/favorite")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public void addTemplateToFavorite(
      @PathParam("templateId") String templateId, @QueryParam("accountId") String accountId) {
    final User user = UserThreadLocal.get();
    personalizationService.addFavoriteTemplate(templateId, accountId, user.getUuid());
  }

  @DELETE
  @Path("templates/{templateId}/favorite")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public void removeTemplateFromFavorite(
      @PathParam("templateId") String templateId, @QueryParam("accountId") String accountId) {
    final User user = UserThreadLocal.get();
    personalizationService.removeFavoriteTemplate(templateId, accountId, user.getUuid());
  }

  @GET
  @Path("templates/favorite")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public RestResponse<Set<String>> fetchFavoriteTemplates(@QueryParam("accountId") String accountId) {
    final User user = UserThreadLocal.get();
    return new RestResponse<>(personalizationService.fetchFavoriteTemplates(accountId, user.getUuid()));
  }
}
