package io.harness.resourcegroup.remote.resource;

import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.resourcegroup.model.ResourceType;
import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.remote.dto.ResourceTypeDTO;
import io.harness.resourcegroup.remote.mapper.ResourceTypeMapper;
import io.harness.resourcegroup.service.ResourceTypeService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Api("/resourcetype")
@Path("resourcetype")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class HarnessResourceTypeResource {
  ResourceTypeService resourceTypeService;

  @GET
  @ApiOperation(value = "Gets a resource types available at this scope", nickname = "getResourceTypes")
  public ResponseDTO<ResourceTypeDTO> get(@NotNull @QueryParam("scope") Scope scope) {
    List<ResourceType> resourceTypes = resourceTypeService.getResourceTypes(scope);
    return ResponseDTO.newResponse(ResourceTypeMapper.toDTO(resourceTypes));
  }
}
