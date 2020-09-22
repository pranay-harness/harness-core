package io.harness.ngpipeline.overlayinputset.resource;

import static io.harness.utils.PageUtils.getNGPageResponse;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.NGPageResponse;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity.OverlayInputSetEntityKeys;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.ngpipeline.overlayinputset.mappers.OverlayInputSetElementMapper;
import io.harness.ngpipeline.overlayinputset.mappers.OverlayInputSetFilterHelper;
import io.harness.ngpipeline.overlayinputset.services.OverlayInputSetEntityService;
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

@Api("/overlayInputSets")
@Path("/overlayInputSets")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class OverlayInputSetResource {
  private final OverlayInputSetEntityService overlayInputSetEntityService;

  @GET
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets an Overlay InputSet by identifier", nickname = "getOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO> get(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier,
      @QueryParam("deleted") @DefaultValue("false") boolean deleted) {
    Optional<OverlayInputSetEntity> overlayInputSetEntity = overlayInputSetEntityService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    return ResponseDTO.newResponse(
        overlayInputSetEntity.map(OverlayInputSetElementMapper::writeResponseDTO).orElse(null));
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create an Overlay InputSet For Pipeline", nickname = "createOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO> create(@NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier, @NotNull String yaml) {
    OverlayInputSetEntity overlayInputSetEntity = OverlayInputSetElementMapper.toOverlayInputSetEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    OverlayInputSetEntity createdEntity = overlayInputSetEntityService.create(overlayInputSetEntity);
    return ResponseDTO.newResponse(OverlayInputSetElementMapper.writeResponseDTO(createdEntity));
  }

  @PUT
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Update an Overlay InputSet by identifier", nickname = "updateOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO> update(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier, @NotNull String yaml) {
    OverlayInputSetEntity requestInputSetEntity = OverlayInputSetElementMapper.toOverlayInputSetEntityWithIdentifier(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, yaml);
    OverlayInputSetEntity updatedInputSetEntity = overlayInputSetEntityService.update(requestInputSetEntity);
    return ResponseDTO.newResponse(OverlayInputSetElementMapper.writeResponseDTO(updatedInputSetEntity));
  }

  @DELETE
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete an Overlay inputSet by identifier", nickname = "deleteOverlayInputSetForPipeline")
  public ResponseDTO<Boolean> delete(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier) {
    return ResponseDTO.newResponse(overlayInputSetEntityService.delete(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier));
  }

  @PUT
  @Path("{inputSetIdentifier}/upsert")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Upsert an Overlay InputSet by identifier", nickname = "upsertOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO> upsert(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier, @NotNull String yaml) {
    OverlayInputSetEntity requestInputSetEntity = OverlayInputSetElementMapper.toOverlayInputSetEntityWithIdentifier(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, yaml);
    OverlayInputSetEntity updatedInputSetEntity = overlayInputSetEntityService.upsert(requestInputSetEntity);
    return ResponseDTO.newResponse(OverlayInputSetElementMapper.writeResponseDTO(updatedInputSetEntity));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets Overlay InputSets list for a pipeline", nickname = "getOverlayInputSetsListForPipeline")
  public ResponseDTO<NGPageResponse<OverlayInputSetResponseDTO>> listInputSetsForPipeline(
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier, @QueryParam("sort") List<String> sort) {
    Criteria criteria = OverlayInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, OverlayInputSetEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    Page<OverlayInputSetResponseDTO> inputSetList =
        overlayInputSetEntityService.list(criteria, pageRequest).map(OverlayInputSetElementMapper::writeResponseDTO);
    return ResponseDTO.newResponse(getNGPageResponse(inputSetList));
  }
}
