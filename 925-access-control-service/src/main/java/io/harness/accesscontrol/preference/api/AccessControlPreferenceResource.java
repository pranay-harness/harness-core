package io.harness.accesscontrol.preference.api;

import io.harness.accesscontrol.preference.services.AccessControlPreferenceService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Path("/aclPreferences")
@Api("/aclPreferences")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@OwnedBy(HarnessTeam.PL)
public class AccessControlPreferenceResource {
  private final AccessControlPreferenceService accessControlPreferenceService;

  @PUT
  @InternalApi
  public ResponseDTO<Boolean> upsertAccessControlPreference(
      @QueryParam("accountIdentifier") @NotEmpty String accountIdentifier,
      @QueryParam("enabled") @DefaultValue("true") boolean enabled) {
    return ResponseDTO.newResponse(
        accessControlPreferenceService.upsertAccessControlEnabled(accountIdentifier, enabled));
  }
}