package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.security.annotations.AuthRule;
import software.wings.security.authentication.SSOConfig;
import software.wings.service.intfc.SSOService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.InputStream;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

@Api(value = "/ng/sso", hidden = true)
@Path("/ng/sso")
@Consumes({MediaType.APPLICATION_JSON, MediaType.MULTIPART_FORM_DATA})
@Produces(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class SSOResourceNG {
  private SSOService ssoService;

  @Inject
  public SSOResourceNG(SSOService ssoService) {
    this.ssoService = ssoService;
  }

  @GET
  @Path("get-access-management")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> getAccountAccessManagementSettings(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(ssoService.getAccountAccessManagementSettings(accountId));
  }

  @POST
  @Path("saml-idp-metadata-upload")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> uploadSamlMetaData(@QueryParam("accountId") String accountId,
      @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("displayName") String displayName,
      @FormDataParam("groupMembershipAttr") String groupMembershipAttr,
      @FormDataParam("authorizationEnabled") Boolean authorizationEnabled,
      @FormDataParam("logoutUrl") String logoutUrl) {
    return new RestResponse<>(ssoService.uploadSamlConfiguration(
        accountId, uploadedInputStream, displayName, groupMembershipAttr, authorizationEnabled, logoutUrl));
  }

  @PUT
  @Path("saml-idp-metadata-upload")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> updateSamlMetaData(@QueryParam("accountId") String accountId,
      @FormDataParam("file") InputStream uploadedInputStream, @FormDataParam("displayName") String displayName,
      @FormDataParam("groupMembershipAttr") String groupMembershipAttr,
      @FormDataParam("authorizationEnabled") Boolean authorizationEnabled,
      @FormDataParam("logoutUrl") String logoutUrl) {
    return new RestResponse<>(ssoService.updateSamlConfiguration(
        accountId, uploadedInputStream, displayName, groupMembershipAttr, authorizationEnabled, logoutUrl));
  }

  @DELETE
  @Path("delete-saml-idp-metadata")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> deleteSamlMetaData(@QueryParam("accountId") String accountId) {
    return new RestResponse<SSOConfig>(ssoService.deleteSamlConfiguration(accountId));
  }
}