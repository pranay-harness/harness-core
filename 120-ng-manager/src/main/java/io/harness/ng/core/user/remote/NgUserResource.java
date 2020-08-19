package io.harness.ng.core.user.remote;

import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserSearchDTO;
import io.harness.ng.core.remote.UserSearchMapper;
import io.harness.ng.core.user.services.api.NgUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import software.wings.beans.User;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("users")
@Path("/users")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NgUserResource {
  private final NgUserService ngUserService;

  @GET
  @ApiOperation(value = "Get users for an account", nickname = "getUsers")
  public ResponseDTO<NGPageResponse<UserSearchDTO>> list(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("searchString") @DefaultValue("") String searchString,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("50") int size,
      @QueryParam("sort") List<String> sort) {
    Pageable pageable = getPageRequest(page, size, sort);
    Page<User> users = ngUserService.list(accountIdentifier, searchString, pageable);
    return ResponseDTO.newResponse(getNGPageResponse(users.map(UserSearchMapper::writeDTO)));
  }
}
