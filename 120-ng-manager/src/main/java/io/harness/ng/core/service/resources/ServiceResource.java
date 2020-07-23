package io.harness.ng.core.service.resources;

import static io.harness.utils.PageUtils.getNGPageResponse;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.service.dto.ServiceRequestDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.mappers.ServiceElementMapper;
import io.harness.ng.core.service.mappers.ServiceFilterHelper;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.utils.PageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/services")
@Path("/services")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class ServiceResource {
  private final ServiceEntityService serviceEntityService;

  @GET
  @Path("{serviceIdentifier}")
  @ApiOperation(value = "Gets a Service by identifier", nickname = "getService")
  public ResponseDTO<Optional<ServiceResponseDTO>> get(@PathParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier) {
    Optional<ServiceEntity> serviceEntity =
        serviceEntityService.get(accountId, orgIdentifier, projectIdentifier, serviceIdentifier);
    return ResponseDTO.newResponse(serviceEntity.map(ServiceElementMapper::writeDTO));
  }

  @POST
  @ApiOperation(value = "Create a Service", nickname = "createService")
  public ResponseDTO<ServiceResponseDTO> create(
      @QueryParam("accountId") String accountId, @NotNull @Valid ServiceRequestDTO serviceRequestDTO) {
    ServiceEntity serviceEntity = ServiceElementMapper.toServiceEntity(accountId, serviceRequestDTO);
    ServiceEntity createdService = serviceEntityService.create(serviceEntity);
    return ResponseDTO.newResponse(ServiceElementMapper.writeDTO(createdService));
  }

  @DELETE
  @Path("{serviceIdentifier}")
  @ApiOperation(value = "Delete a service by identifier", nickname = "deleteService")
  public ResponseDTO<Boolean> delete(@PathParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier) {
    return ResponseDTO.newResponse(
        serviceEntityService.delete(accountId, orgIdentifier, projectIdentifier, serviceIdentifier));
  }

  @PUT
  @ApiOperation(value = "Update a service by identifier", nickname = "updateService")
  public ResponseDTO<Optional<ServiceResponseDTO>> update(
      @QueryParam("accountId") String accountId, @NotNull @Valid ServiceRequestDTO serviceRequestDTO) {
    ServiceEntity requestService = ServiceElementMapper.toServiceEntity(accountId, serviceRequestDTO);
    ServiceEntity updatedService = serviceEntityService.update(requestService);
    return ResponseDTO.newResponse(Optional.ofNullable(ServiceElementMapper.writeDTO(updatedService)));
  }

  @GET
  @ApiOperation(value = "Gets Service list for a project", nickname = "getServiceListForProject")
  public ResponseDTO<NGPageResponse<ServiceResponseDTO>> listServicesForProject(
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier, @QueryParam("sort") List<String> sort) {
    Criteria criteria = ServiceFilterHelper.createCriteria(accountId, orgIdentifier, projectIdentifier);
    Pageable pageRequest = PageUtils.getPageRequest(page, size, sort);
    Page<ServiceResponseDTO> serviceList =
        serviceEntityService.list(criteria, pageRequest).map(ServiceElementMapper::writeDTO);
    return ResponseDTO.newResponse(getNGPageResponse(serviceList));
  }
}
