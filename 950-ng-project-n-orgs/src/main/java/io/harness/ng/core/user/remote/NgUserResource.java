package io.harness.ng.core.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.remote.UserSearchMapper;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Api("users")
@Path("/users")
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(PL)
public class NgUserResource {
  private final NgUserService ngUserService;

  @GET
  @ApiOperation(value = "Get users for an account", nickname = "getUsers")
  public ResponseDTO<PageResponse<UserSearchDTO>> list(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("searchString") @DefaultValue("") String searchString, @BeanParam PageRequest pageRequest) {
    Pageable pageable = getPageRequest(pageRequest);
    Page<User> users = ngUserService.list(accountIdentifier, searchString, pageable);
    return ResponseDTO.newResponse(PageUtils.getNGPageResponse(users.map(UserSearchMapper::writeDTO)));
  }
}
