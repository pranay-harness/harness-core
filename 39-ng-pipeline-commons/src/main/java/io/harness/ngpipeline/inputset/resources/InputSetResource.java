package io.harness.ngpipeline.inputset.resources;

import com.google.inject.Inject;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.beans.entities.MergeInputSetResponse;
import io.harness.ngpipeline.inputset.beans.resource.*;
import io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig;
import io.harness.ngpipeline.inputset.helpers.InputSetEntityValidationHelper;
import io.harness.ngpipeline.inputset.helpers.InputSetMergeHelper;
import io.harness.ngpipeline.inputset.mappers.InputSetElementMapper;
import io.harness.ngpipeline.inputset.mappers.InputSetFilterHelper;
import io.harness.ngpipeline.inputset.services.InputSetEntityService;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity.BaseInputSetEntityKeys;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
import io.harness.overlayinputset.OverlayInputSetConfig;
import io.harness.utils.PageUtils;
import io.swagger.annotations.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.harness.utils.PageUtils.getNGPageResponse;

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
  private final NGPipelineService ngPipelineService;
  private final InputSetMergeHelper inputSetMergeHelper;
  private final InputSetEntityValidationHelper inputSetEntityValidationHelper;

  @GET
  @Path("{inputSetIdentifier}")
  @ApiOperation(value = "Gets an InputSet by identifier", nickname = "getInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> getInputSet(
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted) {
    Optional<BaseInputSetEntity> baseInputSetEntity = inputSetEntityService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    return ResponseDTO.newResponse(
        baseInputSetEntity.map(entity -> InputSetElementMapper.writeInputSetResponseDTO(entity, null)).orElse(null));
  }

  @GET
  @Path("overlay/{inputSetIdentifier}")
  @ApiOperation(value = "Gets an Overlay InputSet by identifier", nickname = "getOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO> getOverlayInputSet(
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted) {
    Optional<BaseInputSetEntity> overlayInputSetEntity = inputSetEntityService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    return ResponseDTO.newResponse(
        overlayInputSetEntity.map(entity -> InputSetElementMapper.writeOverlayResponseDTO(entity, null)).orElse(null));
  }

  @POST
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = InputSetConfig.class,
        dataType = "io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig", paramType = "body")
  })
  @ApiOperation(value = "Create an InputSet For Pipeline", nickname = "createInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO>
  createInputSet(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    InputSetEntity inputSetEntity =
        InputSetElementMapper.toInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);

    inputSetEntity = inputSetMergeHelper.removeRuntimeInputs(inputSetEntity);

    MergeInputSetResponse mergeResponse = inputSetEntityValidationHelper.validateInputSetEntity(inputSetEntity);
    if (mergeResponse.isErrorResponse()) {
      return ResponseDTO.newResponse(InputSetElementMapper.writeInputSetResponseDTO(inputSetEntity, mergeResponse));
    }

    BaseInputSetEntity createdEntity = inputSetEntityService.create(inputSetEntity);
    return ResponseDTO.newResponse(InputSetElementMapper.writeInputSetResponseDTO(createdEntity, null));
  }

  @POST
  @Path("overlay")
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = OverlayInputSetConfig.class,
        dataType = "io.harness.overlayinputset.OverlayInputSetConfig", paramType = "body")
  })
  @ApiOperation(value = "Create an Overlay InputSet For Pipeline", nickname = "createOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO>
  createOverlayInputSet(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
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
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = InputSetConfig.class,
        dataType = "io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig", paramType = "body")
  })
  @ApiOperation(value = "Update an InputSet by identifier", nickname = "updateInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO>
  updateInputSet(@PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    InputSetEntity inputSetEntity = InputSetElementMapper.toInputSetEntityWithIdentifier(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, yaml);

    inputSetEntity = inputSetMergeHelper.removeRuntimeInputs(inputSetEntity);

    MergeInputSetResponse mergeResponse = inputSetEntityValidationHelper.validateInputSetEntity(inputSetEntity);
    if (mergeResponse.isErrorResponse()) {
      return ResponseDTO.newResponse(InputSetElementMapper.writeInputSetResponseDTO(inputSetEntity, mergeResponse));
    }

    BaseInputSetEntity updatedInputSetEntity = inputSetEntityService.update(inputSetEntity);
    return ResponseDTO.newResponse(InputSetElementMapper.writeInputSetResponseDTO(updatedInputSetEntity, null));
  }

  @PUT
  @Path("overlay/{inputSetIdentifier}")
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = OverlayInputSetConfig.class,
        dataType = "io.harness.overlayinputset.OverlayInputSetConfig", paramType = "body")
  })
  @ApiOperation(value = "Update an Overlay InputSet by identifier", nickname = "updateOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTO>
  updateOverlayInputSet(@PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
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
  @ApiOperation(value = "Delete an inputSet by identifier", nickname = "deleteInputSetForPipeline")
  public ResponseDTO<Boolean> delete(
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier) {
    return ResponseDTO.newResponse(inputSetEntityService.delete(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier));
  }

  @GET
  @ApiOperation(value = "Gets InputSets list for a pipeline", nickname = "getInputSetsListForPipeline")
  public ResponseDTO<PageResponse<InputSetSummaryResponseDTO>> listInputSetsForPipeline(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
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
  @ApiOperation(value = "Get template from a pipeline yaml", nickname = "getTemplateFromPipeline")
  public ResponseDTO<InputSetTemplateResponseDTO> getTemplateFromPipeline(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier) {
    Optional<NgPipelineEntity> optionalNgPipelineEntity =
        ngPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (optionalNgPipelineEntity.isPresent()) {
      String pipelineYaml = optionalNgPipelineEntity.get().getYamlPipeline();
      String inputSetTemplate = inputSetMergeHelper.getTemplateFromPipeline(pipelineYaml);
      return ResponseDTO.newResponse(
          InputSetTemplateResponseDTO.builder().inputSetTemplateYaml(inputSetTemplate).build());
    } else {
      throw new InvalidRequestException("Pipeline not found");
    }
  }

  @POST
  @Path("merge")
  @ApiOperation(
      value = "Merges given input sets list on pipeline and return input set template format of applied pipeline",
      nickname = "getMergeInputSetFromPipelineTemplateWithListInput")
  public ResponseDTO<MergeInputSetResponseDTO>
  getMergeInputSetFromPipelineTemplate(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @NotNull @Valid MergeInputSetRequestDTO mergeInputSetRequestDTO) {
    MergeInputSetResponse mergeInputSetResponse =
        inputSetMergeHelper.getMergePipelineYamlFromInputIdentifierList(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, mergeInputSetRequestDTO.getInputSetReferences(), true, useFQNIfErrorResponse);
    return ResponseDTO.newResponse(InputSetElementMapper.toMergeInputSetResponseDTO(mergeInputSetResponse));
  }
}
