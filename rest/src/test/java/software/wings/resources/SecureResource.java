package software.wings.resources;

import software.wings.beans.RestResponse;
import software.wings.beans.User;
import software.wings.security.PermissionAttribute.PermissionScope;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.PublicApi;

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
  @AuthRule("APPLICATION:READ")
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
  @AuthRule("APPLICATION:WRITE")
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
  @AuthRule(value = "ENVIRONMENT:READ", scope = PermissionScope.ENV)
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
  @AuthRule(value = "ENVIRONMENT:WRITE", scope = PermissionScope.ENV)
  public RestResponse<User> envResourceWriteActionOnEnvScope() {
    return new RestResponse<>(UserThreadLocal.get());
  }

  /**
   * Release resource read action on env scope rest response.
   *
   * @return the rest response
   */
  @GET
  @Path("releaseResourceReadActionOnEnvScope")
  @AuthRule(value = "RELEASE:READ", scope = PermissionScope.ENV)
  public RestResponse<User> releaseResourceReadActionOnEnvScope() {
    return new RestResponse<>(UserThreadLocal.get());
  }

  /**
   * Release resource write action on env scope rest response.
   *
   * @return the rest response
   */
  @POST
  @Path("releaseResourceWriteActionOnEnvScope")
  @AuthRule(value = "RELEASE:WRITE", scope = PermissionScope.ENV)
  public RestResponse<User> releaseResourceWriteActionOnEnvScope() {
    return new RestResponse<>(UserThreadLocal.get());
  }

  @GET
  @Path("delegateAuth")
  @DelegateAuth
  public RestResponse<String> delegateAuth() {
    return new RestResponse<>("test");
  }
}
