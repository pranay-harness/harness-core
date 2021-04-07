package io.harness.ng.authenticationsettings.resources;

import com.amazonaws.util.IOUtils;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.exception.WingsException;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.impl.AuthenticationSettingsService;
import io.harness.rest.RestResponse;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Multipart;
import software.wings.app.MainConfiguration;
import software.wings.security.authentication.SSOConfig;

import java.io.InputStream;

@Api("authentication-settings")
@Path("/authentication-settings")
@Produces(MediaType.APPLICATION_JSON)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsResource {
  AuthenticationSettingsService authenticationSettingsService;
  private final MainConfiguration mainConfiguration;

  @GET
  @Path("/")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get authentication settings for an account", nickname = "getAuthenticationSettings")
  public RestResponse<AuthenticationSettingsResponse> getAuthenticationSettings(
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier) {
    AuthenticationSettingsResponse response =
        authenticationSettingsService.getAuthenticationSettings(accountIdentifier);
    return new RestResponse<>(response);
  }

  @Multipart
  @POST
  @Path("/saml-metadata-upload")
  @Timed
  @ExceptionMetered
  public RestResponse<SSOConfig> uploadSamlMetaData(@QueryParam("accountId") String accountId,
                                                    @FormDataParam("file") InputStream uploadedInputStream,
                                                    @FormDataParam("file") FormDataContentDisposition fileDetail, @FormDataParam("displayName") String displayName,
                                                    @FormDataParam("groupMembershipAttr") String groupMembershipAttr,
                                                    @FormDataParam("authorizationEnabled") Boolean authorizationEnabled,
                                                    @FormDataParam("logoutUrl") String logoutUrl) {
    try {
      byte[] bytes = IOUtils.toByteArray(
              new BoundedInputStream(uploadedInputStream, mainConfiguration.getFileUploadLimits().getCommandUploadLimit()));
      final MultipartBody.Part formData =
              MultipartBody.Part.createFormData("file", null, RequestBody.create(MultipartBody.FORM, bytes));
      SSOConfig response = authenticationSettingsService.uploadSAMLMetadata(
              accountId, formData, displayName, groupMembershipAttr, authorizationEnabled, logoutUrl);
      return new RestResponse<>(response);
    } catch (WingsException we) {
      throw we;
    } catch (Exception e) {
      throw new GeneralException("Error while publishing command", e);
    }
    //    SSOConfig response = authenticationSettingsService.uploadSAMLMetadata(accountId, uploadedInputStream,
    //    displayName, groupMembershipAttr, authorizationEnabled, logoutUrl);
  }
}
