package software.wings.resources;

import software.wings.beans.RestResponse;
import software.wings.beans.User;
import software.wings.security.PermissionAttribute.ResourceType;
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
  @AuthRule(ResourceType.APPLICATION)
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
  @AuthRule(ResourceType.APPLICATION)
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
  @AuthRule(ResourceType.APPLICATION)
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
  @AuthRule(ResourceType.ENVIRONMENT)
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
  @AuthRule(value = ResourceType.ENVIRONMENT)
  public RestResponse<User> envResourceWriteActionOnEnvScope() {
    return new RestResponse<>(UserThreadLocal.get());
  }

  @GET
  @Path("delegateAuth")
  @AuthRule(ResourceType.DELEGATE)
  @DelegateAuth
  public RestResponse<String> delegateAuth() {
    return new RestResponse<>("test");
  }
}
