package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_AUTHENTICATION_SETTINGS;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapTestResponse;
import software.wings.beans.sso.OauthSettings;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.LoginTypeResponse;
import software.wings.security.authentication.LoginTypeResponse.LoginTypeResponseBuilder;
import software.wings.security.authentication.SSOConfig;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.SSOService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.io.InputStream;
import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotBlank;

@Api(value = "/ng/sso", hidden = true)
@Path("/ng/sso")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@Slf4j
public class SSOResourceNG {
  private SSOService ssoService;

  @Inject
  public SSOResourceNG(SSOService ssoService) {
    this.ssoService = ssoService;
  }

  @GET
  @Path("access-management/{accountId}")
  @Timed
  @AuthRule(permissionType = LOGGED_IN)
  @ExceptionMetered
  public RestResponse<SSOConfig> getAccountAccessManagementSettings(@PathParam("accountId") String accountId) {
    return new RestResponse<>(ssoService.getAccountAccessManagementSettings(accountId));
  }
}
