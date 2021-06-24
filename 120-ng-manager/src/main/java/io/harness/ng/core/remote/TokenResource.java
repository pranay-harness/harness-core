package io.harness.ng.core.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.TokenService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.TokenDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.Instant;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api("token")
@Path("token")
@Produces({"application/json", "application/yaml", "text/plain"})
@Consumes({"application/json", "application/yaml", "text/plain"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
@OwnedBy(PL)
public class TokenResource {
  @Inject private TokenService tokenService;

  @POST
  @ApiOperation(value = "Create token", nickname = "createToken")
  public ResponseDTO<String> createToken(@Valid TokenDTO tokenDTO) {
    String token = tokenService.createToken(tokenDTO);
    return ResponseDTO.newResponse(token);
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update token", nickname = "updateToken")
  public ResponseDTO<TokenDTO> updateToken(@PathParam("identifier") String identifier, @Valid TokenDTO tokenDTO) {
    TokenDTO token = tokenService.updateToken(tokenDTO);
    return ResponseDTO.newResponse(token);
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete token", nickname = "deleteToken")
  public ResponseDTO<Boolean> deleteToken(@PathParam("identifier") String identifier) {
    boolean isDeleted = tokenService.revokeToken(identifier);
    return ResponseDTO.newResponse(isDeleted);
  }

  @POST
  @Path("rotate/{identifier}")
  @ApiOperation(value = "Rotate token", nickname = "rotateToken")
  public ResponseDTO<String> rotateToken(
      @PathParam("identifier") String identifier, @QueryParam("rotateTimestamp") Long rotateTimestamp) {
    String token = tokenService.rotateToken(identifier, Instant.ofEpochMilli(rotateTimestamp));
    return ResponseDTO.newResponse(token);
  }
}
