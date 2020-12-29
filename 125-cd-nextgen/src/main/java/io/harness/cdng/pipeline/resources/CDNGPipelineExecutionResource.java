package io.harness.cdng.pipeline.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.cdng.pipeline.beans.CDPipelineValidationInfo;
import io.harness.cdng.pipeline.beans.dto.CDPipelineValidationInfoDTO;
import io.harness.cdng.pipeline.helpers.NGPipelineExecuteHelper;
import io.harness.cdng.pipeline.mappers.PipelineValidationMapper;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.MergeInputSetRequestDTO;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.pms.contracts.ambiance.TriggeredBy;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.IOException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("pipelines")
@Path("pipelines/execute")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class CDNGPipelineExecutionResource {
  private final NGPipelineExecuteHelper ngPipelineExecuteHelper;

  private static final TriggeredBy TRIGGERED_BY = TriggeredBy.newBuilder()
                                                      .setUuid("lv0euRhKRCyiXWzS7pOg6g")
                                                      .putExtraInfo("email", "admin@harness.io")
                                                      .setIdentifier("Admin")
                                                      .build();

  @GET
  @Path("/validate")
  @ApiOperation(value = "Validate a Pipeline", nickname = "validatePipeline")
  public ResponseDTO<CDPipelineValidationInfoDTO> validatePipeline(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId) throws IOException {
    log.info("Validating pipeline");
    CDPipelineValidationInfo cdPipelineValidationInfo =
        ngPipelineExecuteHelper.validatePipeline(pipelineId, accountId, orgId, projectId).orElse(null);
    if (cdPipelineValidationInfo == null) {
      return ResponseDTO.newResponse(null);
    }
    return ResponseDTO.newResponse(PipelineValidationMapper.writePipelineValidationDto(cdPipelineValidationInfo));
  }

  @POST
  @Path("/{identifier}")
  @ApiImplicitParams({
    @ApiImplicitParam(
        dataTypeClass = NgPipeline.class, dataType = "io.harness.cdng.pipeline.CDPipeline", paramType = "body")
  })
  @ApiOperation(
      value = "Execute a pipeline with inputSet pipeline yaml", nickname = "postPipelineExecuteWithInputSetYaml")
  public ResponseDTO<NGPipelineExecutionResponseDTO>
  runPipelineWithInputSetPipelineYaml(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam("identifier") @NotEmpty String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @ApiParam(hidden = true, type = "") String inputSetPipelineYaml) {
    return ResponseDTO.newResponse(ngPipelineExecuteHelper.runPipelineWithInputSetPipelineYaml(accountId, orgIdentifier,
        projectIdentifier, pipelineIdentifier, inputSetPipelineYaml, null, useFQNIfErrorResponse, TRIGGERED_BY));
  }

  @POST
  @Path("/{identifier}/inputSetList")
  @ApiOperation(
      value = "Execute a pipeline with input set references list", nickname = "postPipelineExecuteWithInputSetList")
  public ResponseDTO<NGPipelineExecutionResponseDTO>
  runPipelineWithInputSetIdentifierList(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam("identifier") @NotEmpty String pipelineIdentifier,
      @QueryParam("useFQNIfError") @DefaultValue("false") boolean useFQNIfErrorResponse,
      @NotNull @Valid MergeInputSetRequestDTO mergeInputSetRequestDTO) {
    return ResponseDTO.newResponse(
        ngPipelineExecuteHelper.runPipelineWithInputSetReferencesList(accountId, orgIdentifier, projectIdentifier,
            pipelineIdentifier, mergeInputSetRequestDTO.getInputSetReferences(), useFQNIfErrorResponse, TRIGGERED_BY));
  }
}
