package io.harness.pms.ngpipeline.inputset.resources;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.merger.helpers.TemplateHelper.removeRuntimeInputFromYaml;
import static io.harness.utils.PageUtils.getNGPageResponse;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.gitsync.interceptor.GitEntityDeleteInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityUpdateInfoDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.pms.annotations.PipelineServiceAuth;
import io.harness.pms.inputset.InputSetErrorWrapperDTOPMS;
import io.harness.pms.inputset.MergeInputSetResponseDTOPMS;
import io.harness.pms.inputset.MergeInputSetTemplateRequestDTO;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntity.InputSetEntityKeys;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetListTypePMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetSummaryResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetTemplateResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.MergeInputSetRequestDTOPMS;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetElementMapper;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetFilterHelper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTOPMS;
import io.harness.pms.rbac.PipelineRbacPermissions;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
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
@PipelineServiceAuth
@Slf4j
public class InputSetResourcePMS {
  private final PMSInputSetService pmsInputSetService;
  private final ValidateAndMergeHelper validateAndMergeHelper;

  @GET
  @Path("{inputSetIdentifier}")
  @ApiOperation(value = "Gets an InputSet by identifier", nickname = "getInputSetForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<InputSetResponseDTOPMS> getInputSet(
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info(String.format("Retrieving input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    Optional<InputSetEntity> inputSetEntity = pmsInputSetService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    String version = "0";
    if (inputSetEntity.isPresent()) {
      version = inputSetEntity.get().getVersion().toString();
    }

    InputSetResponseDTOPMS inputSet = PMSInputSetElementMapper.toInputSetResponseDTOPMS(inputSetEntity.orElseThrow(
        ()
            -> new InvalidRequestException(String.format(
                "InputSet with the given ID: %s does not exist or has been deleted", inputSetIdentifier))));

    return ResponseDTO.newResponse(version, inputSet);
  }

  @GET
  @Path("overlay/{inputSetIdentifier}")
  @ApiOperation(value = "Gets an Overlay InputSet by identifier", nickname = "getOverlayInputSetForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<OverlayInputSetResponseDTOPMS> getOverlayInputSet(
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info(String.format(
        "Retrieving overlay input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    Optional<InputSetEntity> inputSetEntity = pmsInputSetService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    String version = "0";
    if (inputSetEntity.isPresent()) {
      version = inputSetEntity.get().getVersion().toString();
    }

    OverlayInputSetResponseDTOPMS overlayInputSet =
        PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(inputSetEntity.orElseThrow(
            ()
                -> new InvalidRequestException(String.format(
                    "InputSet with the given ID: %s does not exist or has been deleted", inputSetIdentifier))));

    return ResponseDTO.newResponse(version, overlayInputSet);
  }

  @POST
  @ApiOperation(value = "Create an InputSet For Pipeline", nickname = "createInputSetForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  public ResponseDTO<InputSetResponseDTOPMS> createInputSet(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @QueryParam("pipelineBranch") String pipelineBranch, @QueryParam("pipelineRepoID") String pipelineRepoID,
      @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo, @NotNull @ApiParam(hidden = true) String yaml) {
    yaml = removeRuntimeInputFromYaml(yaml);
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    log.info(String.format("Create input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        entity.getIdentifier(), pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));

    InputSetErrorWrapperDTOPMS errorWrapperDTO = validateAndMergeHelper.validateInputSet(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, pipelineBranch, pipelineRepoID);
    if (errorWrapperDTO != null) {
      return ResponseDTO.newResponse(PMSInputSetElementMapper.toInputSetResponseDTOPMS(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, errorWrapperDTO));
    }

    InputSetEntity createdEntity = pmsInputSetService.create(entity);
    return ResponseDTO.newResponse(
        createdEntity.getVersion().toString(), PMSInputSetElementMapper.toInputSetResponseDTOPMS(createdEntity));
  }

  @POST
  @Path("overlay")
  @ApiOperation(value = "Create an Overlay InputSet For Pipeline", nickname = "createOverlayInputSetForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  public ResponseDTO<OverlayInputSetResponseDTOPMS> createOverlayInputSet(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo, @NotNull @ApiParam(hidden = true) String yaml) {
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityForOverlay(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    log.info(
        String.format("Create overlay input set with identifier %s for pipeline %s in project %s, org %s, account %s",
            entity.getIdentifier(), pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));

    Map<String, String> invalidReferences = validateAndMergeHelper.validateOverlayInputSet(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
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
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  public ResponseDTO<InputSetResponseDTOPMS> updateInputSet(@HeaderParam(IF_MATCH) String ifMatch,
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @QueryParam("pipelineBranch") String pipelineBranch, @QueryParam("pipelineRepoID") String pipelineRepoID,
      @BeanParam GitEntityUpdateInfoDTO gitEntityInfo, @NotNull @ApiParam(hidden = true) String yaml) {
    log.info(String.format("Updating input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    yaml = removeRuntimeInputFromYaml(yaml);

    InputSetErrorWrapperDTOPMS errorWrapperDTO = validateAndMergeHelper.validateInputSet(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, pipelineBranch, pipelineRepoID);
    if (errorWrapperDTO != null) {
      return ResponseDTO.newResponse(PMSInputSetElementMapper.toInputSetResponseDTOPMS(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, errorWrapperDTO));
    }

    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    InputSetEntity entityWithVersion = entity.withVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    InputSetEntity updatedEntity = pmsInputSetService.update(entityWithVersion, ChangeType.MODIFY);
    return ResponseDTO.newResponse(
        updatedEntity.getVersion().toString(), PMSInputSetElementMapper.toInputSetResponseDTOPMS(updatedEntity));
  }

  @PUT
  @Path("overlay/{inputSetIdentifier}")
  @ApiOperation(value = "Update an Overlay InputSet by identifier", nickname = "updateOverlayInputSetForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  public ResponseDTO<OverlayInputSetResponseDTOPMS> updateOverlayInputSet(@HeaderParam(IF_MATCH) String ifMatch,
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @BeanParam GitEntityUpdateInfoDTO gitEntityInfo, @NotNull @ApiParam(hidden = true) String yaml) {
    log.info(
        String.format("Updating overlay input set with identifier %s for pipeline %s in project %s, org %s, account %s",
            inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityForOverlay(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    InputSetEntity entityWithVersion = entity.withVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);

    Map<String, String> invalidReferences = validateAndMergeHelper.validateOverlayInputSet(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    if (!invalidReferences.isEmpty()) {
      return ResponseDTO.newResponse(
          PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(entityWithVersion, true, invalidReferences));
    }

    InputSetEntity updatedEntity = pmsInputSetService.update(entityWithVersion, ChangeType.MODIFY);
    return ResponseDTO.newResponse(
        updatedEntity.getVersion().toString(), PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(updatedEntity));
  }

  @DELETE
  @Path("{inputSetIdentifier}")
  @ApiOperation(value = "Delete an inputSet by identifier", nickname = "deleteInputSetForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_DELETE)
  public ResponseDTO<Boolean> delete(@HeaderParam(IF_MATCH) String ifMatch,
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @BeanParam GitEntityDeleteInfoDTO entityDeleteInfo) {
    log.info(String.format("Deleting input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    return ResponseDTO.newResponse(pmsInputSetService.delete(accountId, orgIdentifier, projectIdentifier,
        pipelineIdentifier, inputSetIdentifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  @GET
  @ApiOperation(value = "Gets InputSets list for a pipeline", nickname = "getInputSetsListForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<PageResponse<InputSetSummaryResponseDTOPMS>> listInputSetsForPipeline(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @QueryParam("inputSetType") @DefaultValue("ALL") InputSetListTypePMS inputSetListType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam(NGResourceFilterConstants.SORT_KEY) List<String> sort,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info(String.format("Get List of input sets for pipeline %s in project %s, org %s, account %s",
        pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    Criteria criteria = PMSInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetListType, searchTerm, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, InputSetEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    Page<InputSetSummaryResponseDTOPMS> inputSetList =
        pmsInputSetService.list(criteria, pageRequest, accountId, orgIdentifier, projectIdentifier)
            .map(PMSInputSetElementMapper::toInputSetSummaryResponseDTOPMS);
    return ResponseDTO.newResponse(getNGPageResponse(inputSetList));
  }

  @GET
  @Path("template")
  @ApiOperation(value = "Get template from a pipeline yaml", nickname = "getTemplateFromPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<InputSetTemplateResponseDTOPMS> getTemplateFromPipeline(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info(String.format("Get template for pipeline %s in project %s, org %s, account %s", pipelineIdentifier,
        projectIdentifier, orgIdentifier, accountId));
    String pipelineTemplateYaml =
        validateAndMergeHelper.getPipelineTemplate(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    return ResponseDTO.newResponse(
        InputSetTemplateResponseDTOPMS.builder().inputSetTemplateYaml(pipelineTemplateYaml).build());
  }

  @POST
  @Path("merge")
  @ApiOperation(
      value = "Merges given input sets list on pipeline and return input set template format of applied pipeline",
      nickname = "getMergeInputSetFromPipelineTemplateWithListInput")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  public ResponseDTO<MergeInputSetResponseDTOPMS>
  getMergeInputSetFromPipelineTemplate(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @QueryParam("pipelineBranch") String pipelineBranch, @QueryParam("pipelineRepoID") String pipelineRepoID,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @NotNull @Valid MergeInputSetRequestDTOPMS mergeInputSetRequestDTO) {
    List<String> inputSetReferences = mergeInputSetRequestDTO.getInputSetReferences();
    String mergedYaml = validateAndMergeHelper.getMergeInputSetFromPipelineTemplate(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, inputSetReferences, pipelineBranch, pipelineRepoID);
    String fullYaml = "";
    if (mergeInputSetRequestDTO.isWithMergedPipelineYaml()) {
      fullYaml = validateAndMergeHelper.mergeInputSetIntoPipeline(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, mergedYaml, pipelineBranch, pipelineRepoID);
    }
    return ResponseDTO.newResponse(MergeInputSetResponseDTOPMS.builder()
                                       .isErrorResponse(false)
                                       .pipelineYaml(mergedYaml)
                                       .completePipelineYaml(fullYaml)
                                       .build());
  }

  @POST
  @Path("mergeWithTemplateYaml")
  @ApiOperation(
      value = "Merges given runtime input yaml on pipeline and return input set template format of applied pipeline",
      nickname = "getMergeInputSetFromPipelineTemplate")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_VIEW)
  // TODO(Naman): Correct PipelineServiceClient when modifying this api
  public ResponseDTO<MergeInputSetResponseDTOPMS>
  getMergeInputSetFromPipelineTemplate(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @QueryParam("pipelineBranch") String pipelineBranch, @QueryParam("pipelineRepoID") String pipelineRepoID,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @NotNull @Valid MergeInputSetTemplateRequestDTO mergeInputSetTemplateRequestDTO) {
    String fullYaml = validateAndMergeHelper.mergeInputSetIntoPipeline(accountId, orgIdentifier, projectIdentifier,
        pipelineIdentifier, mergeInputSetTemplateRequestDTO.getRuntimeInputYaml(), pipelineBranch, pipelineRepoID);
    return ResponseDTO.newResponse(MergeInputSetResponseDTOPMS.builder()
                                       .isErrorResponse(false)
                                       .pipelineYaml(mergeInputSetTemplateRequestDTO.getRuntimeInputYaml())
                                       .completePipelineYaml(fullYaml)
                                       .build());
  }
}
