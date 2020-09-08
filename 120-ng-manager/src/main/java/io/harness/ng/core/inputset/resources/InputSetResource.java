package io.harness.ng.core.inputset.resources;

import static io.harness.utils.PageUtils.getNGPageResponse;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.NGPageResponse;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity.CDInputSetEntityKeys;
import io.harness.cdng.inputset.beans.resource.InputSetRequestDTO;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.mappers.CDInputSetElementMapper;
import io.harness.cdng.inputset.mappers.CDInputSetFilterHelper;
import io.harness.cdng.inputset.services.CDInputSetEntityService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
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

@Api("/inputSets")
@Path("/inputSets")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class InputSetResource {
  private final CDInputSetEntityService cdInputSetEntityService;

  @GET
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets an InputSet by identifier", nickname = "getInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> get(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("pipelineIdentifier") String pipelineIdentifier,
      @QueryParam("deleted") @DefaultValue("false") boolean deleted) {
    Optional<CDInputSetEntity> cdInputSetEntity = cdInputSetEntityService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    return ResponseDTO.newResponse(cdInputSetEntity.map(CDInputSetElementMapper::writeResponseDTO).orElse(null));
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create an InputSet For Pipeline", nickname = "createInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> create(@QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Valid InputSetRequestDTO inputSetRequestDTO) {
    CDInputSetEntity cdInputSetEntity =
        CDInputSetElementMapper.toCDInputSetEntity(accountId, orgIdentifier, projectIdentifier, inputSetRequestDTO);
    CDInputSetEntity createdEntity = cdInputSetEntityService.create(cdInputSetEntity);
    return ResponseDTO.newResponse(CDInputSetElementMapper.writeResponseDTO(createdEntity));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Update an InputSet by identifier", nickname = "updateInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> update(@QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Valid InputSetRequestDTO inputSetRequestDTO) {
    CDInputSetEntity requestInputSetEntity =
        CDInputSetElementMapper.toCDInputSetEntity(accountId, orgIdentifier, projectIdentifier, inputSetRequestDTO);
    CDInputSetEntity updatedInputSetEntity = cdInputSetEntityService.update(requestInputSetEntity);
    return ResponseDTO.newResponse(CDInputSetElementMapper.writeResponseDTO(updatedInputSetEntity));
  }

  @DELETE
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete an inputSet by identifier", nickname = "deleteInputSetForPipeline")
  public ResponseDTO<Boolean> delete(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("pipelineIdentifier") String pipelineIdentifier) {
    return ResponseDTO.newResponse(cdInputSetEntityService.delete(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier));
  }

  @PUT
  @Path("upsert")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Upsert an inputSet by identifier", nickname = "upsertInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> upsert(@QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Valid InputSetRequestDTO inputSetRequestDTO) {
    CDInputSetEntity cdInputSetEntity =
        CDInputSetElementMapper.toCDInputSetEntity(accountId, orgIdentifier, projectIdentifier, inputSetRequestDTO);
    CDInputSetEntity upsertedInputSetEntity = cdInputSetEntityService.upsert(cdInputSetEntity);
    return ResponseDTO.newResponse(CDInputSetElementMapper.writeResponseDTO(upsertedInputSetEntity));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets InputSets list for a pipeline", nickname = "getInputSetsListForPipeline")
  public ResponseDTO<NGPageResponse<InputSetResponseDTO>> listInputSetsForPipeline(
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size,
      @QueryParam("accountId") String accountId, @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("projectIdentifier") String projectIdentifier,
      @QueryParam("pipelineIdentifier") String pipelineIdentifier, @QueryParam("sort") List<String> sort) {
    Criteria criteria = CDInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, CDInputSetEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    Page<InputSetResponseDTO> inputSetList =
        cdInputSetEntityService.list(criteria, pageRequest).map(CDInputSetElementMapper::writeResponseDTO);
    return ResponseDTO.newResponse(getNGPageResponse(inputSetList));
  }
}
