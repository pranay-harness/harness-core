/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.rest.RestResponse;
import io.harness.security.annotations.DelegateAuth;
import io.harness.security.annotations.PublicApi;

import software.wings.beans.User;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.Scope;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 8/31/16.
 */
@Path("secure-resources")
@Produces("application/json")
public class SecureResource {
  /**
   * Public api response.
   *
   * @return the response
   */
  @GET
  @Path("publicApiAuthTokenNotRequired")
  @PublicApi
  public Response publicApi() {
    return Response.ok().build();
  }

  /**
   * Non public api rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("NonPublicApi")
  @Scope(ResourceType.APPLICATION)
  public RestResponse<User> NonPublicApi() {
    return new RestResponse<>(UserThreadLocal.get());
  }

  /**
   * App resource read action on app scope rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("appResourceReadActionOnAppScope")
  @Scope(ResourceType.APPLICATION)
  public RestResponse<User> appResourceReadActionOnAppScope() {
    return new RestResponse<>(UserThreadLocal.get());
  }

  /**
   * App resource write action on app scope rest response.
   *
   * @return the rest response
   */
  @POST
  @Path("appResourceWriteActionOnAppScope")
  @Scope(ResourceType.APPLICATION)
  public RestResponse<User> appResourceWriteActionOnAppScope() {
    return new RestResponse<>(UserThreadLocal.get());
  }

  /**
   * Env resource read action on env scope rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("envResourceReadActionOnEnvScope")
  @Scope(ResourceType.ENVIRONMENT)
  public RestResponse<User> envResourceReadActionOnEnvScope() {
    return new RestResponse<>(UserThreadLocal.get());
  }

  /**
   * Env resource write action on env scope rest response.
   *
   * @return the rest response
   */
  @POST
  @Path("envResourceWriteActionOnEnvScope")
  @Scope(ResourceType.ENVIRONMENT)
  public RestResponse<User> envResourceWriteActionOnEnvScope() {
    return new RestResponse<>(UserThreadLocal.get());
  }

  @GET
  @Path("delegateAuth")
  @Scope(ResourceType.DELEGATE)
  @DelegateAuth
  public RestResponse<String> delegateAuth() {
    return new RestResponse<>("test");
  }
}
