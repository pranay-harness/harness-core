/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.inputset.resources;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.merger.helpers.InputSetTemplateHelper.removeRuntimeInputFromYaml;
import static io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType.INPUT_SET;
import static io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType.OVERLAY_INPUT_SET;
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
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.gitsync.interceptor.GitEntityDeleteInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityUpdateInfoDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.Status;
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
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetTemplateRequestDTO;
import io.harness.pms.ngpipeline.inputset.beans.resource.InputSetTemplateResponseDTOPMS;
import io.harness.pms.ngpipeline.inputset.beans.resource.MergeInputSetRequestDTOPMS;
import io.harness.pms.ngpipeline.inputset.helpers.ValidateAndMergeHelper;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetElementMapper;
import io.harness.pms.ngpipeline.inputset.mappers.PMSInputSetFilterHelper;
import io.harness.pms.ngpipeline.inputset.service.PMSInputSetService;
import io.harness.pms.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTOPMS;
import io.harness.pms.pipeline.PipelineResourceConstants;
import io.harness.pms.rbac.PipelineRbacPermissions;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PIPELINE)
@Api("/inputSets/V2")
@Path("/inputSets/V2")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })

@Tag(name = "InputSets", description = "Contain APIs corresponding to the Input Sets, including Overlay Input Sets.")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })

@PipelineServiceAuth
@Slf4j
public class InputSetResourceV2 {
  private final PMSInputSetService pmsInputSetService;
  private final ValidateAndMergeHelper validateAndMergeHelper;

  @POST
  @ApiOperation(value = "Create an InputSet For Pipeline", nickname = "createInputSetForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  @Operation(operationId = "postInputSet", summary = "Create an Input Set for a Pipeline",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "If the YAML is valid, returns created Input Set. If not, it sends what is wrong with the YAML")
      })
  public ResponseDTO<InputSetResponseDTOPMS>
  createInputSet(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @Parameter(
                     description = PipelineResourceConstants.ACCOUNT_PARAM_MESSAGE) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @Parameter(
          description = PipelineResourceConstants.ORG_PARAM_MESSAGE) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier @Parameter(
          description = PipelineResourceConstants.PROJECT_PARAM_MESSAGE) String projectIdentifier,
      @Parameter(
          description =
              "Pipeline identifier for the input set. The input set will work only for the pipeline corresponding to this identifier.")
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @QueryParam("pipelineBranch") @Parameter(
          description = "Github branch of the Pipeline for which the Input Set is to be created") String pipelineBranch,
      @QueryParam("pipelineRepoID")
      @Parameter(description = "Github Repo identifier of the Pipeline for which the Input Set is to be created")
      String pipelineRepoID, @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo,
      @RequestBody(required = true,
          description =
              "Input set YAML to be created. The account, org, project, and pipeline identifiers inside the YAML should match the query parameters")
      @NotNull String yaml) {
    yaml = removeRuntimeInputFromYaml(yaml);
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    log.info(String.format("Create input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        entity.getIdentifier(), pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));

    InputSetErrorWrapperDTOPMS errorWrapperDTO = validateAndMergeHelper.validateInputSet(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, pipelineBranch, pipelineRepoID);
    if (errorWrapperDTO != null) {
      ResponseDTO responseDTO = ResponseDTO.newResponse(PMSInputSetElementMapper.toInputSetResponseDTOPMS(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, errorWrapperDTO));
      responseDTO.setStatus(Status.FAILURE);
      return responseDTO;
    }

    InputSetEntity createdEntity = pmsInputSetService.create(entity);
    return ResponseDTO.newResponse(
        createdEntity.getVersion().toString(), PMSInputSetElementMapper.toInputSetResponseDTOPMS(createdEntity));
  }

  @POST
  @Path("overlay")
  @ApiOperation(value = "Create an Overlay InputSet For Pipeline", nickname = "createOverlayInputSetForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  @Operation(operationId = "postOverlayInputSet", summary = "Create an Overlay Input Set for a pipeline",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "If the YAML is valid, returns created Overlay Input Set. If not, it sends what is wrong with the YAML")
      })
  public ResponseDTO<OverlayInputSetResponseDTOPMS>
  createOverlayInputSet(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @Parameter(
          description = PipelineResourceConstants.ACCOUNT_PARAM_MESSAGE) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @Parameter(
          description = PipelineResourceConstants.ORG_PARAM_MESSAGE) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @Parameter(
          description = PipelineResourceConstants.PROJECT_PARAM_MESSAGE) @ProjectIdentifier String projectIdentifier,
      @Parameter(
          description =
              "Pipeline identifier for the overlay input set. The Overlay Input Set will work only for the Pipeline corresponding to this identifier.")
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo,
      @RequestBody(required = true,
          description =
              "Overlay Input Set YAML to be created. The Account, Org, Project, and Pipeline identifiers inside the YAML should match the query parameters")
      @NotNull String yaml) {
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityForOverlay(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    log.info(
        String.format("Create overlay input set with identifier %s for pipeline %s in project %s, org %s, account %s",
            entity.getIdentifier(), pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));

    Map<String, String> invalidReferences = validateAndMergeHelper.validateOverlayInputSet(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    if (!invalidReferences.isEmpty()) {
      ResponseDTO responseDTO = ResponseDTO.newResponse(
          PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(entity, true, invalidReferences));
      responseDTO.setStatus(Status.FAILURE);
      return responseDTO;
    }

    InputSetEntity createdEntity = pmsInputSetService.create(entity);
    return ResponseDTO.newResponse(
        createdEntity.getVersion().toString(), PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(createdEntity));
  }

  @PUT
  @Path("{inputSetIdentifier}")
  @ApiOperation(value = "Update an InputSet by identifier", nickname = "updateInputSetForPipeline")
  @NGAccessControlCheck(resourceType = "PIPELINE", permission = PipelineRbacPermissions.PIPELINE_CREATE_AND_EDIT)
  @Operation(operationId = "putInputSet", summary = "Update Input Set for Pipeline",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "If the YAML is valid, returns the updated Input Set. If not, it sends what is wrong with the YAML")
      })
  public ResponseDTO<InputSetResponseDTOPMS>
  updateInputSet(
      @Parameter(description = PipelineResourceConstants.IF_MATCH_PARAM_MESSAGE) @HeaderParam(IF_MATCH) String ifMatch,
      @Parameter(
          description =
              "Identifier for the Input Set that needs to be updated. An Input Set corresponding to this identifier should already exist.")
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @Parameter(description = PipelineResourceConstants.ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @Parameter(description = PipelineResourceConstants.ORG_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = PipelineResourceConstants.PROJECT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Parameter(
          description =
              "Pipeline Id for the Input Set. The Input Set will work only for the Pipeline corresponding to this Id.")
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @QueryParam("pipelineBranch") @Parameter(
          description = "Github branch of the Pipeline for which the Input Set is to be created") String pipelineBranch,
      @QueryParam("pipelineRepoID")
      @Parameter(description = "Github Repo Id of the Pipeline for which the Input Set is to be created")
      String pipelineRepoID, @BeanParam GitEntityUpdateInfoDTO gitEntityInfo,
      @RequestBody(required = true,
          description =
              "Input set YAML to be updated. The Account, Org, Project, and Pipeline Ids inside the YAML should match the query parameters")
      @NotNull String yaml) {
    log.info(String.format("Updating input set with identifier %s for pipeline %s in project %s, org %s, account %s",
        inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    yaml = removeRuntimeInputFromYaml(yaml);

    InputSetErrorWrapperDTOPMS errorWrapperDTO = validateAndMergeHelper.validateInputSet(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, pipelineBranch, pipelineRepoID);
    if (errorWrapperDTO != null) {
      ResponseDTO responseDTO = ResponseDTO.newResponse(PMSInputSetElementMapper.toInputSetResponseDTOPMS(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, errorWrapperDTO));
      responseDTO.setStatus(Status.FAILURE);
      return responseDTO;
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
  @Operation(operationId = "putOverlayInputSet", summary = "Update an Overlay Input Set for a pipeline",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "If the YAML is valid, returns the updated Overlay Input Set. If not, it sends what is wrong with the YAML")
      })
  public ResponseDTO<OverlayInputSetResponseDTOPMS>
  updateOverlayInputSet(
      @Parameter(description = PipelineResourceConstants.IF_MATCH_PARAM_MESSAGE) @HeaderParam(IF_MATCH) String ifMatch,
      @Parameter(
          description =
              "Identifier for the Overlay Input Set that needs to be updated. An Overlay Input Set corresponding to this identifier should already exist.")
      @PathParam(NGCommonEntityConstants.INPUT_SET_IDENTIFIER_KEY) String inputSetIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @Parameter(
          description = PipelineResourceConstants.ACCOUNT_PARAM_MESSAGE) @AccountIdentifier String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @Parameter(
          description = PipelineResourceConstants.ORG_PARAM_MESSAGE) @OrgIdentifier String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @Parameter(
          description = PipelineResourceConstants.PROJECT_PARAM_MESSAGE) @ProjectIdentifier String projectIdentifier,
      @Parameter(
          description =
              "Pipeline identifier for the Overlay Input Set. The Overlay Input Set will work only for the Pipeline corresponding to this identifier.")
      @NotNull @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) @ResourceIdentifier String pipelineIdentifier,
      @BeanParam GitEntityUpdateInfoDTO gitEntityInfo,
      @RequestBody(required = true,
          description =
              "Overlay Input Set YAML to be updated. The Account, Org, Project, and Pipeline identifiers inside the YAML should match the query parameters, and the Overlay Input Set identifier cannot be changed.")
      @NotNull @ApiParam(hidden = true) String yaml) {
    log.info(
        String.format("Updating overlay input set with identifier %s for pipeline %s in project %s, org %s, account %s",
            inputSetIdentifier, pipelineIdentifier, projectIdentifier, orgIdentifier, accountId));
    InputSetEntity entity = PMSInputSetElementMapper.toInputSetEntityForOverlay(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    InputSetEntity entityWithVersion = entity.withVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);

    Map<String, String> invalidReferences = validateAndMergeHelper.validateOverlayInputSet(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    if (!invalidReferences.isEmpty()) {
      ResponseDTO responseDTO = ResponseDTO.newResponse(
          PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(entityWithVersion, true, invalidReferences));
      responseDTO.setStatus(Status.FAILURE);
      return responseDTO;
    }

    InputSetEntity updatedEntity = pmsInputSetService.update(entityWithVersion, ChangeType.MODIFY);
    return ResponseDTO.newResponse(
        updatedEntity.getVersion().toString(), PMSInputSetElementMapper.toOverlayInputSetResponseDTOPMS(updatedEntity));
  }
}
