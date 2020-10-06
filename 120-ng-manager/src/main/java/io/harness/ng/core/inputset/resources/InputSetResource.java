package io.harness.ng.core.inputset.resources;

import static io.harness.utils.PageUtils.getNGPageResponse;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.NGCommonEntityConstants;
import io.harness.NGConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.MergeInputSetResponse;
import io.harness.cdng.inputset.beans.resource.InputSetListType;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetTemplateResponseDTO;
import io.harness.cdng.inputset.beans.resource.MergeInputSetRequestDTO;
import io.harness.cdng.inputset.beans.resource.MergeInputSetResponseDTO;
import io.harness.cdng.inputset.beans.yaml.CDInputSet;
import io.harness.cdng.inputset.helpers.InputSetEntityValidationHelper;
import io.harness.cdng.inputset.helpers.InputSetMergeHelper;
import io.harness.cdng.inputset.mappers.InputSetElementMapper;
import io.harness.cdng.inputset.mappers.InputSetFilterHelper;
import io.harness.cdng.inputset.services.InputSetEntityService;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.service.PipelineService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity.BaseInputSetEntityKeys;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.overlayinputset.OverlayInputSet;
import io.harness.utils.PageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
import java.util.Map;
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
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class InputSetResource {
  private final InputSetEntityService inputSetEntityService;
  private final PipelineService ngPipelineService;
  private final InputSetMergeHelper inputSetMergeHelper;
  private final InputSetEntityValidationHelper inputSetEntityValidationHelper;

  @GET
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets an InputSet by identifier", nickname = "getInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> getCDInputSet(
      @PathParam(NGConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted) {
    Optional<BaseInputSetEntity> baseInputSetEntity = inputSetEntityService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    return ResponseDTO.newResponse(
        baseInputSetEntity.map(entity -> InputSetElementMapper.writeCDInputSetResponseDTO(entity, null)).orElse(null));
  }

  @GET
  @Path("overlay/{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets an Overlay InputSet by identifier", nickname = "getOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO> getOverlayInputSet(
      @PathParam(NGConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted) {
    Optional<BaseInputSetEntity> overlayInputSetEntity = inputSetEntityService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    return ResponseDTO.newResponse(
        overlayInputSetEntity.map(entity -> InputSetElementMapper.writeOverlayResponseDTO(entity, null)).orElse(null));
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = CDInputSet.class, dataType = "io.harness.cdng.inputset.beans.yaml.CDInputSet",
        paramType = "body")
  })
  @ApiOperation(value = "Create an InputSet For Pipeline", nickname = "createInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO>
  createCDInputSet(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    CDInputSetEntity cdInputSetEntity =
        InputSetElementMapper.toCDInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);

    MergeInputSetResponse mergeResponse = inputSetEntityValidationHelper.validateInputSetEntity(cdInputSetEntity);
    if (mergeResponse.isErrorResponse()) {
      return ResponseDTO.newResponse(InputSetElementMapper.writeCDInputSetResponseDTO(cdInputSetEntity, mergeResponse));
    }

    BaseInputSetEntity createdEntity = inputSetEntityService.create(cdInputSetEntity);
    return ResponseDTO.newResponse(InputSetElementMapper.writeCDInputSetResponseDTO(createdEntity, null));
  }

  @POST
  @Path("overlay")
  @Timed
  @ExceptionMetered
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = OverlayInputSet.class, dataType = "io.harness.overlayinputset.OverlayInputSet",
        paramType = "body")
  })
  @ApiOperation(value = "Create an Overlay InputSet For Pipeline", nickname = "createOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO>
  createOverlayInputSet(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    OverlayInputSetEntity overlayInputSetEntity = InputSetElementMapper.toOverlayInputSetEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);

    Map<String, String> invalidIdentifiers =
        inputSetEntityValidationHelper.validateOverlayInputSetEntity(overlayInputSetEntity);
    if (EmptyPredicate.isNotEmpty(invalidIdentifiers)) {
      return ResponseDTO.newResponse(
          InputSetElementMapper.writeOverlayResponseDTO(overlayInputSetEntity, invalidIdentifiers));
    }

    BaseInputSetEntity createdEntity = inputSetEntityService.create(overlayInputSetEntity);
    return ResponseDTO.newResponse(InputSetElementMapper.writeOverlayResponseDTO(createdEntity, null));
  }

  @PUT
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = CDInputSet.class, dataType = "io.harness.cdng.inputset.beans.yaml.CDInputSet",
        paramType = "body")
  })
  @ApiOperation(value = "Update an InputSet by identifier", nickname = "updateInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO>
  updateCDInputSet(@PathParam(NGConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    CDInputSetEntity cdInputSetEntity = InputSetElementMapper.toCDInputSetEntityWithIdentifier(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, yaml);

    MergeInputSetResponse mergeResponse = inputSetEntityValidationHelper.validateInputSetEntity(cdInputSetEntity);
    if (mergeResponse.isErrorResponse()) {
      return ResponseDTO.newResponse(InputSetElementMapper.writeCDInputSetResponseDTO(cdInputSetEntity, mergeResponse));
    }

    BaseInputSetEntity updatedInputSetEntity = inputSetEntityService.update(cdInputSetEntity);
    return ResponseDTO.newResponse(InputSetElementMapper.writeCDInputSetResponseDTO(updatedInputSetEntity, null));
  }

  @PUT
  @Path("overlay/{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = OverlayInputSet.class, dataType = "io.harness.overlayinputset.OverlayInputSet",
        paramType = "body")
  })
  @ApiOperation(value = "Update an Overlay InputSet by identifier", nickname = "updateOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO>
  updateOverlayInputSet(@PathParam(NGConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    OverlayInputSetEntity overlayInputSetEntity = InputSetElementMapper.toOverlayInputSetEntityWithIdentifier(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, yaml);

    Map<String, String> invalidIdentifiers =
        inputSetEntityValidationHelper.validateOverlayInputSetEntity(overlayInputSetEntity);
    if (EmptyPredicate.isNotEmpty(invalidIdentifiers)) {
      return ResponseDTO.newResponse(
          InputSetElementMapper.writeOverlayResponseDTO(overlayInputSetEntity, invalidIdentifiers));
    }

    BaseInputSetEntity updatedInputSetEntity = inputSetEntityService.update(overlayInputSetEntity);
    return ResponseDTO.newResponse(InputSetElementMapper.writeOverlayResponseDTO(updatedInputSetEntity, null));
  }

  @DELETE
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete an inputSet by identifier", nickname = "deleteInputSetForPipeline")
  public ResponseDTO<Boolean> delete(@PathParam(NGConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier) {
    return ResponseDTO.newResponse(inputSetEntityService.delete(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets InputSets list for a pipeline", nickname = "getInputSetsListForPipeline")
  public ResponseDTO<PageResponse<InputSetSummaryResponseDTO>> listInputSetsForPipeline(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam("inputSetType") @DefaultValue("ALL") InputSetListType inputSetListType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(NGResourceFilterConstants.SORT_KEY) List<String> sort) {
    Criteria criteria = InputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetListType, searchTerm, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, BaseInputSetEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    Page<InputSetSummaryResponseDTO> inputSetList =
        inputSetEntityService.list(criteria, pageRequest).map(InputSetElementMapper::writeSummaryResponseDTO);
    return ResponseDTO.newResponse(getNGPageResponse(inputSetList));
  }

  @GET
  @Path("template")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get template from a pipeline yaml", nickname = "getTemplateFromPipeline")
  public ResponseDTO<InputSetTemplateResponseDTO> getTemplateFromPipeline(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier) {
    Optional<CDPipelineResponseDTO> pipeline =
        ngPipelineService.getPipeline(pipelineIdentifier, accountId, orgIdentifier, projectIdentifier);
    if (pipeline.isPresent()) {
      String pipelineYaml = pipeline.get().getYamlPipeline();
      String inputSetTemplate = inputSetMergeHelper.getTemplateFromPipeline(pipelineYaml);
      return ResponseDTO.newResponse(
          InputSetTemplateResponseDTO.builder().inputSetTemplateYaml(inputSetTemplate).build());
    } else {
      throw new InvalidRequestException("Pipeline not found");
    }
  }

  @POST
  @Path("merge")
  @Timed
  @ExceptionMetered
  @ApiOperation(
      value = "Merges given input sets list on pipeline and return input set template format of applied pipeline",
      nickname = "getMergeInputSetFromPipelineTemplateWithListInput")
  public ResponseDTO<MergeInputSetResponseDTO>
  getMergeInputSetFromPipelineTemplate(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @NotNull @Valid MergeInputSetRequestDTO mergeInputSetRequestDTO) {
    MergeInputSetResponse mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputIdentifierList(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, mergeInputSetRequestDTO.getInputSetReferences(), true, useFQNIfErrorResponse);
    return ResponseDTO.newResponse(InputSetElementMapper.toMergeInputSetResponseDTO(mergeInputSetResponse));
  }
}
