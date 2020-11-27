package io.harness.ngpipeline.pipeline.resources;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngpipeline.common.RestQueryFilterParser;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity;
import io.harness.ngpipeline.pipeline.beans.entities.NgPipelineEntity.PipelineNGKeys;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineResponseDTO;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineSummaryResponseDTO;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.ngpipeline.pipeline.mappers.NgPipelineFilterHelper;
import io.harness.ngpipeline.pipeline.mappers.PipelineDtoMapper;
import io.harness.ngpipeline.pipeline.service.NGPipelineService;
// import io.harness.ngtriggers.utils.TriggerUtils;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.*;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@Api("pipelines")
@Path("pipelines")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class NGPipelineResource {
  private final NGPipelineService ngPipelineService;
  private final RestQueryFilterParser restQueryFilterParser;
  //  private final TriggerUtils triggerUtils;

  @GET
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Gets a pipeline by identifier", nickname = "getPipeline")
  public ResponseDTO<NGPipelineResponseDTO> getPipelineByIdentifier(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId) {
    log.info("Get pipeline");
    Optional<NgPipelineEntity> ngPipelineEntity = ngPipelineService.get(accountId, orgId, projectId, pipelineId, false);
    return ResponseDTO.newResponse(ngPipelineEntity.get().getVersion().toString(),
        ngPipelineEntity.map(PipelineDtoMapper::writePipelineDto).orElse(null));
  }

  @GET
  @Path("summary/{pipelineIdentifier}")
  @ApiOperation(value = "Gets a pipeline by identifier", nickname = "getPipelineSummary")
  public ResponseDTO<NGPipelineSummaryResponseDTO> getPipelineSummaryByIdentifier(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId) {
    log.info("Get pipeline");
    Optional<NgPipelineEntity> ngPipelineEntity = ngPipelineService.get(accountId, orgId, projectId, pipelineId, false);
    return ResponseDTO.newResponse(ngPipelineEntity.get().getVersion().toString(),
        ngPipelineEntity.map(PipelineDtoMapper::preparePipelineSummary).orElse(null));
  }

  @GET
  @ApiOperation(value = "Gets Pipeline list", nickname = "getPipelineList")
  public ResponseDTO<Page<NGPipelineSummaryResponseDTO>> getListOfPipelines(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @QueryParam("filter") String filterQuery, @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("25") int size, @QueryParam("sort") List<String> sort,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    log.info("Get List of pipelines");
    Criteria criteria = restQueryFilterParser.getCriteriaFromFilterQuery(filterQuery, NgPipeline.class);
    criteria.andOperator(
        NgPipelineFilterHelper.createCriteriaForGetList(accountId, orgId, projectId, searchTerm, false));
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, PipelineNGKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    Page<NGPipelineSummaryResponseDTO> pipelines =
        ngPipelineService.list(criteria, pageRequest).map(PipelineDtoMapper::preparePipelineSummary);

    return ResponseDTO.newResponse(pipelines);
  }

  @POST
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = NgPipeline.class,
        dataType = "io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline", paramType = "body")
  })
  @ApiOperation(value = "Create a Pipeline", nickname = "postPipeline")
  public ResponseDTO<String>
  createPipeline(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    log.info("Creating pipeline");
    NgPipelineEntity ngPipelineEntity = PipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    NgPipelineEntity createdEntity = ngPipelineService.create(ngPipelineEntity);

    return ResponseDTO.newResponse(createdEntity.getVersion().toString(), createdEntity.getIdentifier());
  }

  @PUT
  @Path("/{pipelineIdentifier}")
  @ApiImplicitParams({
    @ApiImplicitParam(dataTypeClass = NgPipeline.class,
        dataType = "io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline", paramType = "body")
  })
  @ApiOperation(value = "Update a Pipeline", nickname = "putPipeline")
  public ResponseDTO<String>
  updatePipeline(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId,
      @NotNull @ApiParam(hidden = true, type = "") String yaml) {
    log.info("Updating pipeline");
    NgPipelineEntity ngPipelineEntity = PipelineDtoMapper.toPipelineEntity(accountId, orgId, projectId, yaml);
    if (!ngPipelineEntity.getIdentifier().equals(pipelineId)) {
      throw new InvalidRequestException("Pipeline identifier in URL does not match pipeline identifier in yaml");
    }
    ngPipelineEntity.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    NgPipelineEntity updatedEntity = ngPipelineService.update(ngPipelineEntity);

    return ResponseDTO.newResponse(updatedEntity.getVersion().toString(), updatedEntity.getIdentifier());
  }

  @DELETE
  @Path("/{pipelineIdentifier}")
  @ApiOperation(value = "Delete a pipeline", nickname = "softDeletePipeline")
  public ResponseDTO<Boolean> deletePipeline(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgId,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectId,
      @PathParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineId) {
    return ResponseDTO.newResponse(ngPipelineService.delete(
        accountId, orgId, projectId, pipelineId, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }
}
