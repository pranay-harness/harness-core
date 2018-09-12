package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.RestResponse;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapTestResponse;
import software.wings.exception.InvalidRequestException;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SSOService;

import java.io.InputStream;
import java.util.Collection;
import javax.validation.Valid;
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

@Api("sso")
@Path("/sso")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.SSO)
@AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
public class SSOResource {
  private SSOService ssoService;
  private FeatureFlagService featureFlagService;

  @SuppressFBWarnings("URF_UNREAD_FIELD")
  @Inject
  public SSOResource(SSOService ssoService, FeatureFlagService featureFlagService) {
    this.ssoService = ssoService;
    this.featureFlagService = featureFlagService;
  }

  @POST
  @Path("saml-idp-metadata-upload")
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  public RestResponse<SSOConfig> uploadSamlMetaData(@QueryParam("accountId") String accountId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("displayName") String displayName) {
    return new RestResponse<>(ssoService.uploadSamlConfiguration(accountId, uploadedInputStream, displayName));
  }

  @DELETE
  @Path("delete-saml-idp-metadata")
  @Timed
  @ExceptionMetered
  public RestResponse<SSOConfig> deleteSamlMetaData(@QueryParam("accountId") String accountId) {
    return new RestResponse<SSOConfig>(ssoService.deleteSamlConfiguration(accountId));
  }

  @PUT
  @Path("assign-auth-mechanism")
  @Timed
  @ExceptionMetered
  public RestResponse<SSOConfig> setAuthMechanism(@QueryParam("accountId") String accountId,
      @QueryParam("authMechanism") AuthenticationMechanism authenticationMechanism) {
    return new RestResponse<SSOConfig>(ssoService.setAuthenticationMechanism(accountId, authenticationMechanism));
  }

  @GET
  @Path("access-management/{accountId}")
  @Timed
  @ExceptionMetered
  public RestResponse<SSOConfig> getAccountAccessManagementSettings(@PathParam("accountId") String accountId) {
    return new RestResponse<SSOConfig>(ssoService.getAccountAccessManagementSettings(accountId));
  }

  @POST
  @Path("ldap/settings")
  @Timed
  @ExceptionMetered
  public RestResponse<LdapSettings> createLdapSettings(
      @QueryParam("accountId") @NotBlank String accountId, @Valid LdapSettings settings) {
    if (!settings.getAccountId().equals(accountId)) {
      throw new InvalidRequestException("accountId in the query parameter and request body don't match");
    }
    return new RestResponse<>(ssoService.createLdapSettings(settings));
  }

  @PUT
  @Path("ldap/settings")
  @Timed
  @ExceptionMetered
  public RestResponse<LdapSettings> updateLdapSettings(
      @QueryParam("accountId") @NotBlank String accountId, @Valid LdapSettings settings) {
    if (!settings.getAccountId().equals(accountId)) {
      throw new InvalidRequestException("accountId in the query parameter and request body don't match");
    }
    return new RestResponse<>(ssoService.updateLdapSettings(settings));
  }

  @GET
  @Path("ldap/settings")
  @Timed
  @ExceptionMetered
  public RestResponse<LdapSettings> getLdapSettings(@QueryParam("accountId") @NotBlank String accountId) {
    return new RestResponse<>(ssoService.getLdapSettings(accountId));
  }

  @DELETE
  @Path("ldap/settings")
  @Timed
  @ExceptionMetered
  public RestResponse<LdapSettings> deleteLdapSettings(@QueryParam("accountId") @NotBlank String accountId) {
    return new RestResponse<>(ssoService.deleteLdapSettings(accountId));
  }

  @POST
  @Path("ldap/settings/test/connection")
  @Timed
  @ExceptionMetered
  public RestResponse<LdapTestResponse> validateLdapConnectionSettings(
      @QueryParam("accountId") @NotBlank String accountId, @Valid LdapSettings settings) {
    return new RestResponse<>(ssoService.validateLdapConnectionSettings(settings, accountId));
  }

  @POST
  @Path("ldap/settings/test/user")
  @Timed
  @ExceptionMetered
  public RestResponse<LdapTestResponse> validateLdapUserSettings(
      @QueryParam("accountId") @NotBlank String accountId, @Valid LdapSettings settings) {
    return new RestResponse<>(ssoService.validateLdapUserSettings(settings, accountId));
  }

  @POST
  @Path("ldap/settings/test/group")
  @Timed
  @ExceptionMetered
  public RestResponse<LdapTestResponse> validateLdapGroupSettings(
      @QueryParam("accountId") @NotBlank String accountId, @Valid LdapSettings settings) {
    return new RestResponse<>(ssoService.validateLdapGroupSettings(settings, accountId));
  }

  @GET
  @Path("ldap/{ldapId}/search/group")
  @Timed
  @ExceptionMetered
  public RestResponse<Collection<LdapGroupResponse>> searchLdapGroups(@PathParam("ldapId") String ldapId,
      @QueryParam("accountId") @NotBlank String accountId, @QueryParam("q") @NotBlank String query) {
    Collection<LdapGroupResponse> groups = ssoService.searchGroupsByName(ldapId, query);
    return new RestResponse<>(groups);
  }
}
