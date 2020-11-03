package io.harness.ng.core.service.resources;

import static io.harness.utils.PageUtils.getNGPageResponse;
import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static software.wings.beans.Service.ServiceKeys;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.beans.PageResponse;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
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
  public ResponseDTO<ServiceResponseDTO> get(@PathParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("deleted") @DefaultValue("false") boolean deleted) {
    Optional<ServiceEntity> serviceEntity =
        serviceEntityService.get(accountId, orgIdentifier, projectIdentifier, serviceIdentifier, deleted);
    return ResponseDTO.newResponse(
        serviceEntity.get().getVersion().toString(), serviceEntity.map(ServiceElementMapper::writeDTO).orElse(null));
  }

  @POST
  @ApiOperation(value = "Create a Service", nickname = "createService")
  public ResponseDTO<ServiceResponseDTO> create(
      @QueryParam("accountId") String accountId, @NotNull @Valid ServiceRequestDTO serviceRequestDTO) {
    ServiceEntity serviceEntity = ServiceElementMapper.toServiceEntity(accountId, serviceRequestDTO);
    ServiceEntity createdService = serviceEntityService.create(serviceEntity);
    return ResponseDTO.newResponse(
        createdService.getVersion().toString(), ServiceElementMapper.writeDTO(createdService));
  }

  @POST
  @Path("/batch")
  @ApiOperation(value = "Create Services", nickname = "createServices")
  public ResponseDTO<PageResponse<ServiceResponseDTO>> createServices(
      @QueryParam("accountId") String accountId, @NotNull @Valid List<ServiceRequestDTO> serviceRequestDTOs) {
    List<ServiceEntity> serviceEntities =
        serviceRequestDTOs.stream()
            .map(serviceRequestDTO -> ServiceElementMapper.toServiceEntity(accountId, serviceRequestDTO))
            .collect(Collectors.toList());
    Page<ServiceEntity> createdServices = serviceEntityService.bulkCreate(accountId, serviceEntities);
    return ResponseDTO.newResponse(
        getNGPageResponse(createdServices.map(serviceEntity -> ServiceElementMapper.writeDTO(serviceEntity))));
  }

  @DELETE
  @Path("{serviceIdentifier}")
  @ApiOperation(value = "Delete a service by identifier", nickname = "deleteService")
  public ResponseDTO<Boolean> delete(@HeaderParam(IF_MATCH) String ifMatch,
      @PathParam("serviceIdentifier") String serviceIdentifier, @QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier) {
    return ResponseDTO.newResponse(serviceEntityService.delete(accountId, orgIdentifier, projectIdentifier,
        serviceIdentifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  @PUT
  @ApiOperation(value = "Update a service by identifier", nickname = "updateService")
  public ResponseDTO<ServiceResponseDTO> update(@HeaderParam(IF_MATCH) String ifMatch,
      @QueryParam("accountId") String accountId, @NotNull @Valid ServiceRequestDTO serviceRequestDTO) {
    ServiceEntity requestService = ServiceElementMapper.toServiceEntity(accountId, serviceRequestDTO);
    requestService.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    ServiceEntity updatedService = serviceEntityService.update(requestService);
    return ResponseDTO.newResponse(
        updatedService.getVersion().toString(), ServiceElementMapper.writeDTO(updatedService));
  }

  @PUT
  @Path("upsert")
  @ApiOperation(value = "Upsert a service by identifier", nickname = "upsertService")
  public ResponseDTO<ServiceResponseDTO> upsert(@HeaderParam(IF_MATCH) String ifMatch,
      @QueryParam("accountId") String accountId, @NotNull @Valid ServiceRequestDTO serviceRequestDTO) {
    ServiceEntity requestService = ServiceElementMapper.toServiceEntity(accountId, serviceRequestDTO);
    requestService.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    ServiceEntity upsertedService = serviceEntityService.upsert(requestService);
    return ResponseDTO.newResponse(
        upsertedService.getVersion().toString(), ServiceElementMapper.writeDTO(upsertedService));
  }

  @GET
  @ApiOperation(value = "Gets Service list for a project", nickname = "getServiceListForProject")
  public ResponseDTO<PageResponse<ServiceResponseDTO>> listServicesForProject(
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier, @QueryParam("sort") List<String> sort) {
    Criteria criteria =
        ServiceFilterHelper.createCriteriaForGetList(accountId, orgIdentifier, projectIdentifier, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, ServiceKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    Page<ServiceResponseDTO> serviceList =
        serviceEntityService.list(criteria, pageRequest).map(ServiceElementMapper::writeDTO);
    return ResponseDTO.newResponse(getNGPageResponse(serviceList));
  }
}
