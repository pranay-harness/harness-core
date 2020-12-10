package io.harness.pms.ngpipeline.inputset.resources;

import static io.harness.utils.PageUtils.getNGPageResponse;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.inputset.helpers.MergeHelper;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;
import io.harness.pms.ngpipeline.inputset.beans.resource.*;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetElementMapper;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetFilterHelper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTOPMS;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

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
public class InputSetResourcePMS {
  private final PMSPipelineService pmsPipelineService;
  private final PMSInputSetService pmsInputSetService;

  @GET
  @Path("{inputSetIdentifier}")
  @ApiOperation(value = "Gets an InputSet by identifier", nickname = "getInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTOPMS> getInputSet(
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted) {
    Optional<InputSetEntity> inputSetEntity = pmsInputSetService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    String version = "0";
    if (inputSetEntity.isPresent()) {
      version = inputSetEntity.get().getVersion().toString();
    }
    return ResponseDTO.newResponse(
        version, inputSetEntity.map(PMSInputSetElementMapper::toInputSetResponseDTOPMS).orElse(null));
  }

  @GET
  @Path("overlay/{inputSetIdentifier}")
  @ApiOperation(value = "Gets an Overlay InputSet by identifier", nickname = "getOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTOPMS> getOverlayInputSet(
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted) {
    Optional<InputSetEntity> inputSetEntity = pmsInputSetService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    String version = "0";
    if (inputSetEntity.isPresent()) {
      version = inputSetEntity.get().getVersion().toString();
    }
    return ResponseDTO.newResponse(
        version, inputSetEntity.map(PMSInputSetElementMapper::toOverlayInputSetResponseDTOPMS).orElse(null));
  }

  @POST
  @ApiOperation(value = "Create an InputSet For Pipeline", nickname = "createInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTOPMS> createInputSet(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true) String yaml) {
    try {
      yaml = MergeHelper.removeRuntimeInputFromYaml(yaml);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not clear ${input} fields from yaml : " + e.getMessage());
    }

    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (pipelineEntity.isPresent()) {
      String pipelineYaml = pipelineEntity.get().getYaml();
      try {
        InputSetErrorWrapperDTOPMS errorWrapperDTO = MergeHelper.getErrorMap(pipelineYaml, yaml);
        if (errorWrapperDTO != null) {
          return ResponseDTO.newResponse(PMSInputSetElementMapper.toInputSetResponseDTOPMS(
              accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, errorWrapperDTO));
        }
      } catch (IOException e) {
        throw new InvalidRequestException("Invalid input set yaml");
      }
    } else {
      throw new InvalidRequestException("Pipeline does not exist");
    }

    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    InputSetEntity createdEntity = pmsInputSetService.create(entity);
    return ResponseDTO.newResponse(
        createdEntity.getVersion().toString(), PMSInputSetElementMapper.toInputSetResponseDTOPMS(createdEntity));
  }

  @POST
  @Path("overlay")
  @ApiOperation(value = "Create an Overlay InputSet For Pipeline", nickname = "createOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTOPMS> createOverlayInputSet(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true) String yaml) {
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityForOverlay(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);

    List<String> inputSetReferences = entity.getInputSetReferences();
    if (inputSetReferences.isEmpty()) {
      throw new InvalidRequestException("Input Set References can't be empty");
    }
    List<Optional<InputSetEntity>> inputSets = new ArrayList<>();
    inputSetReferences.forEach(identifier
        -> inputSets.add(pmsInputSetService.get(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, false)));
    Map<String, String> invalidReferences = MergeHelper.getInvalidInputSetReferences(inputSets, inputSetReferences);
    if (!invalidReferences.isEmpty()) {
      return ResponseDTO.newResponse(
          PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(entity, true, invalidReferences));
    }

    InputSetEntity createdEntity = pmsInputSetService.create(entity);
    return ResponseDTO.newResponse(
        createdEntity.getVersion().toString(), PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(createdEntity));
  }

  @PUT
  @Path("{inputSetIdentifier}")
  @ApiOperation(value = "Update an InputSet by identifier", nickname = "updateInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTOPMS> updateInputSet(@HeaderParam(IF_MATCH) String ifMatch,
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true) String yaml) {
    try {
      yaml = MergeHelper.removeRuntimeInputFromYaml(yaml);
    } catch (IOException e) {
      throw new InvalidRequestException("Could not clear ${input} fields from yaml : " + e.getMessage());
    }

    Optional<PipelineEntity> pipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (pipelineEntity.isPresent()) {
      String pipelineYaml = pipelineEntity.get().getYaml();
      try {
        InputSetErrorWrapperDTOPMS errorWrapperDTO = MergeHelper.getErrorMap(pipelineYaml, yaml);
        if (errorWrapperDTO != null) {
          return ResponseDTO.newResponse(PMSInputSetElementMapper.toInputSetResponseDTOPMS(
              accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, errorWrapperDTO));
        }
      } catch (IOException e) {
        throw new InvalidRequestException("Invalid input set yaml");
      }
    } else {
      throw new InvalidRequestException("Pipeline does not exist");
    }

    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    entity.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    InputSetEntity createdEntity = pmsInputSetService.update(entity);
    return ResponseDTO.newResponse(
        createdEntity.getVersion().toString(), PMSInputSetElementMapper.toInputSetResponseDTOPMS(createdEntity));
  }

  @PUT
  @Path("overlay/{inputSetIdentifier}")
  @ApiOperation(value = "Update an Overlay InputSet by identifier", nickname = "updateOverlayInputSetForPipeline")
  public ResponseDTO<OverlayInputSetResponseDTOPMS> updateOverlayInputSet(@HeaderParam(IF_MATCH) String ifMatch,
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @NotNull @ApiParam(hidden = true) String yaml) {
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityForOverlay(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    entity.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);

    List<String> inputSetReferences = entity.getInputSetReferences();
    if (inputSetReferences.isEmpty()) {
      throw new InvalidRequestException("Input Set References can't be empty");
    }
    List<Optional<InputSetEntity>> inputSets = new ArrayList<>();
    inputSetReferences.forEach(identifier
        -> inputSets.add(pmsInputSetService.get(
            accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, false)));
    Map<String, String> invalidReferences = MergeHelper.getInvalidInputSetReferences(inputSets, inputSetReferences);
    if (!invalidReferences.isEmpty()) {
      return ResponseDTO.newResponse(
          PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(entity, true, invalidReferences));
    }

    InputSetEntity createdEntity = pmsInputSetService.update(entity);
    return ResponseDTO.newResponse(
        createdEntity.getVersion().toString(), PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(createdEntity));
  }

  @DELETE
  @Path("{inputSetIdentifier}")
  @ApiOperation(value = "Delete an inputSet by identifier", nickname = "deleteInputSetForPipeline")
  public ResponseDTO<Boolean> delete(@HeaderParam(IF_MATCH) String ifMatch,
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier) {
    return ResponseDTO.newResponse(pmsInputSetService.delete(accountId, orgIdentifier, projectIdentifier,
        pipelineIdentifier, inputSetIdentifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  @GET
  @ApiOperation(value = "Gets InputSets list for a pipeline", nickname = "getInputSetsListForPipeline")
  public ResponseDTO<PageResponse<InputSetSummaryResponseDTOPMS>> listInputSetsForPipeline(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam("inputSetType") @DefaultValue("ALL") InputSetListTypePMS inputSetListType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(NGResourceFilterConstants.SORT_KEY) List<String> sort) {
    Criteria criteria = PMSInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetListType, searchTerm, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, InputSetEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    Page<InputSetSummaryResponseDTOPMS> inputSetList =
        pmsInputSetService.list(criteria, pageRequest).map(PMSInputSetElementMapper::toInputSetSummaryResponseDTOPMS);
    return ResponseDTO.newResponse(getNGPageResponse(inputSetList));
  }

  @GET
  @Path("template")
  @ApiOperation(value = "Get template from a pipeline yaml", nickname = "getTemplateFromPipeline")
  public ResponseDTO<InputSetTemplateResponseDTOPMS> getTemplateFromPipeline(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier) {
    Optional<PipelineEntity> optionalPipelineEntity =
        pmsPipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (optionalPipelineEntity.isPresent()) {
      String pipelineYaml = optionalPipelineEntity.get().getYaml();
      try {
        String pipelineTemplateYaml = MergeHelper.createTemplateFromPipeline(pipelineYaml);
        return ResponseDTO.newResponse(
            InputSetTemplateResponseDTOPMS.builder().inputSetTemplateYaml(pipelineTemplateYaml).build());
      } catch (IOException e) {
        throw new InvalidRequestException("Could not convert pipeline to template");
      }
    } else {
      return ResponseDTO.newResponse(InputSetTemplateResponseDTOPMS.builder().build());
    }
  }

  @POST
  @Path("merge")
  @ApiOperation(
      value = "Merges given input sets list on pipeline and return input set template format of applied pipeline",
      nickname = "getMergeInputSetFromPipelineTemplateWithListInput")
  public ResponseDTO<MergeInputSetResponseDTOPMS>
  getMergeInputSetFromPipelineTemplate(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @NotNull @Valid MergeInputSetRequestDTOPMS mergeInputSetRequestDTO) {
    String pipelineTemplate = getTemplateFromPipeline(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier)
                                  .getData()
                                  .getInputSetTemplateYaml();
    List<String> inputSetReferences = mergeInputSetRequestDTO.getInputSetReferences();
    List<String> inputSetYamlList = new ArrayList<>();
    inputSetReferences.forEach(identifier -> {
      Optional<InputSetEntity> entity =
          pmsInputSetService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, identifier, false);
      if (!entity.isPresent()) {
        throw new InvalidRequestException(identifier + " does not exist");
      }
      InputSetEntity inputSet = entity.get();
      if (inputSet.getInputSetEntityType() == InputSetEntityType.INPUT_SET) {
        inputSetYamlList.add(entity.get().getYaml());
      } else {
        List<String> overlayReferences = inputSet.getInputSetReferences();
        overlayReferences.forEach(id -> {
          Optional<InputSetEntity> entity2 =
              pmsInputSetService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, id, false);
          if (!entity2.isPresent()) {
            throw new InvalidRequestException(id + " does not exist");
          }
          inputSetYamlList.add(entity2.get().getYaml());
        });
      }
    });
    try {
      String mergedYaml = MergeHelper.mergeInputSets(pipelineTemplate, inputSetYamlList);
      return ResponseDTO.newResponse(
          MergeInputSetResponseDTOPMS.builder().isErrorResponse(false).pipelineYaml(mergedYaml).build());
    } catch (IOException e) {
      throw new InvalidRequestException("Could not merge input sets : " + e.getMessage());
    }
  }
}