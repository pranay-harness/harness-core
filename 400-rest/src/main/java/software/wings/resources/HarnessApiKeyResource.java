/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.beans.ClientType;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.LearningEngineAuth;

import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.HarnessApiKeyService;
import software.wings.utils.AccountPermissionUtils;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Api("harness-api-keys")
@Path("/harness-api-keys")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(ResourceType.API_KEY)
@AuthRule(permissionType = PermissionType.LOGGED_IN)
/**
 *
 * @author rktummala on 03/07/2019
 */
public class HarnessApiKeyResource {
  private HarnessApiKeyService harnessApiKeyService;
  private AccountPermissionUtils accountPermissionUtils;

  @Inject
  public HarnessApiKeyResource(
      HarnessApiKeyService harnessApiKeyService, AccountPermissionUtils accountPermissionUtils) {
    this.harnessApiKeyService = harnessApiKeyService;
    this.accountPermissionUtils = accountPermissionUtils;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> generate(@NotNull String clientType) {
    RestResponse response = accountPermissionUtils.checkIfHarnessUser("User not allowed to perform operation");
    if (response == null) {
      response = new RestResponse<>(harnessApiKeyService.generate(clientType));
    }
    return response;
  }

  @DELETE
  @Path("{clientType}")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> delete(@NotEmpty @PathParam("clientType") String clientType) {
    RestResponse response = accountPermissionUtils.checkIfHarnessUser("User not allowed to perform operation");
    if (response == null) {
      response = new RestResponse<>(harnessApiKeyService.delete(clientType));
    }
    return response;
  }

  @GET
  @Path("{clientType}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> get(@NotEmpty @PathParam("clientType") String clientType) {
    RestResponse response = accountPermissionUtils.checkIfHarnessUser("User not allowed to perform operation");
    if (response == null) {
      response = new RestResponse<>(harnessApiKeyService.get(clientType));
    }
    return response;
  }

  @GET
  @Path("validate")
  @Timed
  @ExceptionMetered
  @LearningEngineAuth
  public RestResponse<Boolean> validate(
      @NotEmpty @QueryParam("clientType") String clientType, @NotEmpty @QueryParam("apiKey") String apiKey) {
    return new RestResponse<>(
        harnessApiKeyService.validateHarnessClientApiRequest(ClientType.valueOf(clientType), apiKey));
  }
}
