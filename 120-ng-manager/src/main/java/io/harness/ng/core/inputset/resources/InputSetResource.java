package io.harness.ng.core.inputset.resources;

import static io.harness.utils.PageUtils.getNGPageResponse;

import com.google.common.io.Resources;
import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.NGPageResponse;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.CDInputSetEntity.CDInputSetEntityKeys;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetTemplateResponseDTO;
import io.harness.cdng.inputset.beans.resource.MergeInputSetRequestDTO;
import io.harness.cdng.inputset.beans.resource.MergeInputSetResponseDTO;
import io.harness.cdng.inputset.mappers.CDInputSetElementMapper;
import io.harness.cdng.inputset.mappers.CDInputSetFilterHelper;
import io.harness.cdng.inputset.services.CDInputSetEntityService;
import io.harness.cdng.pipeline.beans.dto.CDPipelineResponseDTO;
import io.harness.cdng.pipeline.service.PipelineService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
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
  private PipelineService ngPipelineService;

  @GET
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets an InputSet by identifier", nickname = "getInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> get(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier,
      @QueryParam("deleted") @DefaultValue("false") boolean deleted) {
    Optional<CDInputSetEntity> cdInputSetEntity = cdInputSetEntityService.get(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, deleted);
    return ResponseDTO.newResponse(cdInputSetEntity.map(CDInputSetElementMapper::writeResponseDTO).orElse(null));
  }

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create an InputSet For Pipeline", nickname = "createInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> create(@NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier, @NotNull String yaml) {
    CDInputSetEntity cdInputSetEntity = CDInputSetElementMapper.toCDInputSetEntity(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    CDInputSetEntity createdEntity = cdInputSetEntityService.create(cdInputSetEntity);
    return ResponseDTO.newResponse(CDInputSetElementMapper.writeResponseDTO(createdEntity));
  }

  @PUT
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Update an InputSet by identifier", nickname = "updateInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> update(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier, @NotNull String yaml) {
    CDInputSetEntity requestInputSetEntity = CDInputSetElementMapper.toCDInputSetEntityWithIdentifier(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, yaml);
    CDInputSetEntity updatedInputSetEntity = cdInputSetEntityService.update(requestInputSetEntity);
    return ResponseDTO.newResponse(CDInputSetElementMapper.writeResponseDTO(updatedInputSetEntity));
  }

  @DELETE
  @Path("{inputSetIdentifier}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete an inputSet by identifier", nickname = "deleteInputSetForPipeline")
  public ResponseDTO<Boolean> delete(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier) {
    return ResponseDTO.newResponse(cdInputSetEntityService.delete(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier));
  }

  @PUT
  @Path("{inputSetIdentifier}/upsert")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Upsert an inputSet by identifier", nickname = "upsertInputSetForPipeline")
  public ResponseDTO<InputSetResponseDTO> upsert(@PathParam("inputSetIdentifier") String inputSetIdentifier,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier, @NotNull String yaml) {
    CDInputSetEntity cdInputSetEntity = CDInputSetElementMapper.toCDInputSetEntityWithIdentifier(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, inputSetIdentifier, yaml);
    CDInputSetEntity upsertedInputSetEntity = cdInputSetEntityService.upsert(cdInputSetEntity);
    return ResponseDTO.newResponse(CDInputSetElementMapper.writeResponseDTO(upsertedInputSetEntity));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Gets InputSets list for a pipeline", nickname = "getInputSetsListForPipeline")
  public ResponseDTO<NGPageResponse<InputSetSummaryResponseDTO>> listInputSetsForPipeline(
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("100") int size,
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier, @QueryParam("sort") List<String> sort) {
    Criteria criteria = CDInputSetFilterHelper.createCriteriaForGetList(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, CDInputSetEntityKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    Page<InputSetSummaryResponseDTO> inputSetList =
        cdInputSetEntityService.list(criteria, pageRequest).map(CDInputSetElementMapper::writeSummaryResponseDTO);
    return ResponseDTO.newResponse(getNGPageResponse(inputSetList));
  }

  @GET
  @Path("template")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get template from a pipeline yaml", nickname = "getTemplateFromPipeline")
  public ResponseDTO<InputSetTemplateResponseDTO> getTemplateFromPipeline(
      @NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier) {
    Optional<CDPipelineResponseDTO> pipeline =
        ngPipelineService.getPipeline(pipelineIdentifier, accountId, orgIdentifier, projectIdentifier);
    if (pipeline.isPresent()) {
      String pipelineYaml = pipeline.get().getYamlPipeline();
      String inputSetTemplate = cdInputSetEntityService.getTemplateFromPipeline(pipelineYaml);
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
      nickname = "getMergeInputSetFromPipelineTemplate")
  public ResponseDTO<MergeInputSetResponseDTO>
  getMergeInputSetFromPipelineTemplate(@NotNull @QueryParam("accountIdentifier") String accountId,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("pipelineIdentifier") String pipelineIdentifier,
      @NotNull @Valid MergeInputSetRequestDTO mergeInputSetRequestDTO) {
    // currently returning a dummy response
    String dummyFilename = "dummyInputSetTemplate.yaml";
    ClassLoader classLoader = this.getClass().getClassLoader();
    String content = null;
    try {
      content =
          Resources.toString(Objects.requireNonNull(classLoader.getResource(dummyFilename)), StandardCharsets.UTF_8);
    } catch (IOException e) {
      // do nothing
    }
    return ResponseDTO.newResponse(MergeInputSetResponseDTO.builder().pipelineYaml(content).build());
  }
}
