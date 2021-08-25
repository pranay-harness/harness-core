package io.harness.template.resources;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityUpdateInfoDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.template.beans.TemplateApplyRequestDTO;
import io.harness.template.beans.TemplateResponseDTO;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.mappers.NGTemplateDtoMapper;
import io.harness.template.services.NGTemplateService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Optional;
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

@OwnedBy(CDC)
@Api("templates")
@Path("templates")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@Slf4j
public class NGTemplateResource {
  private final NGTemplateService templateService;

  @GET
  @Path("{templateIdentifier}")
  @ApiOperation(value = "Gets Template", nickname = "getTemplate")
  public ResponseDTO<TemplateResponseDTO> get(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam("templateIdentifier") @ResourceIdentifier String templateIdentifier,
      @QueryParam(NGCommonEntityConstants.VERSION_LABEL_KEY) String versionLabel,
      @QueryParam(NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    // if label is not given, return stable template

    log.info(
        String.format("Retrieving Template with identifier %s and versionLabel %s in project %s, org %s, account %s",
            templateIdentifier, versionLabel, projectId, orgId, accountId));
    Optional<TemplateEntity> templateEntity =
        templateService.get(accountId, orgId, projectId, templateIdentifier, versionLabel, deleted);

    String version = "0";
    if (templateEntity.isPresent()) {
      version = templateEntity.get().getVersion().toString();
    }
    TemplateResponseDTO templateResponseDTO = NGTemplateDtoMapper.writeTemplateResponseDto(templateEntity.orElseThrow(
        ()
            -> new InvalidRequestException(String.format(
                "Template with the given Identifier: %s and versionLabel: %s does not exist or has been deleted",
                templateIdentifier, versionLabel))));
    return ResponseDTO.newResponse(version, templateResponseDTO);
  }

  @POST
  @ApiOperation(value = "Creates a Template", nickname = "createTemplate")
  public ResponseDTO<TemplateResponseDTO> create(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo, @NotNull String templateYaml) {
    TemplateEntity templateEntity = NGTemplateDtoMapper.toTemplateEntity(accountId, orgId, projectId, templateYaml);
    log.info(String.format("Creating Template with identifier %s with label %s in project %s, org %s, account %s",
        templateEntity.getIdentifier(), templateEntity.getVersionLabel(), projectId, orgId, accountId));

    // TODO(archit): Add schema validations
    TemplateEntity createdTemplate = templateService.create(templateEntity);
    return ResponseDTO.newResponse(
        createdTemplate.getVersion().toString(), NGTemplateDtoMapper.writeTemplateResponseDto(createdTemplate));
  }

  @PUT
  @Path("/{templateIdentifier}/{label}")
  @ApiOperation(value = "Updating stable template label", nickname = "updateStableTemplate")
  public ResponseDTO<String> updateStableTemplate(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam("templateIdentifier") @ResourceIdentifier String templateIdentifier,
      @PathParam(NGCommonEntityConstants.VERSION_LABEL_KEY) String label) {
    return null;
  }

  @PUT
  @Path("/update/{templateIdentifier}/{versionLabel}")
  @ApiOperation(value = "Updating existing template label", nickname = "updateExistingTemplateLabel")
  public ResponseDTO<TemplateResponseDTO> updateExistingTemplateLabel(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam("templateIdentifier") @ResourceIdentifier String templateIdentifier,
      @PathParam(NGCommonEntityConstants.VERSION_LABEL_KEY) String versionLabel,
      @BeanParam GitEntityUpdateInfoDTO gitEntityInfo, @NotNull String templateYaml) {
    TemplateEntity templateEntity = NGTemplateDtoMapper.toTemplateEntity(
        accountId, orgId, projectId, templateIdentifier, versionLabel, templateYaml);
    log.info(String.format("Updating Template with identifier %s with label %s in project %s, org %s, account %s",
        templateEntity.getIdentifier(), templateEntity.getVersionLabel(), projectId, orgId, accountId));
    templateEntity = templateEntity.withVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);

    // TODO(archit): Add schema validations
    TemplateEntity createdTemplate = templateService.updateTemplateEntity(templateEntity, ChangeType.MODIFY);
    return ResponseDTO.newResponse(
        createdTemplate.getVersion().toString(), NGTemplateDtoMapper.writeTemplateResponseDto(createdTemplate));
  }

  @DELETE
  @Path("/{templateIdentifier}/{label}")
  @ApiOperation(value = "Deletes template label", nickname = "deleteTemplateLabel")
  public ResponseDTO<String> deleteTemplate(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam("templateIdentifier") @ResourceIdentifier String templateIdentifier,
      @NotNull @PathParam(NGCommonEntityConstants.VERSION_LABEL_KEY) String templateLabel) {
    return null;
  }

  @POST
  @Path("/list")
  @ApiOperation(value = "Gets all template list", nickname = "getTemplateList")
  // will return non deleted templates only
  public ResponseDTO<Page<TemplateResponseDTO>> listTemplates(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size,
      @QueryParam("sort") List<String> sort, @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("filterIdentifier") String filterIdentifier, FilterPropertiesDTO filterProperties) {
    return null;
  }

  @GET
  @Path("/templateInputs/{templateIdentifier}")
  @ApiOperation(value = "Gets template input set yaml", nickname = "getTemplateInputSetYaml")
  public ResponseDTO<String> getTemplateInputsYaml(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam("templateIdentifier") @ResourceIdentifier String templateIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.VERSION_LABEL_KEY) String templateLabel) {
    // if label not given, then consider stable template label
    // returns templateInputs yaml
    return null;
  }

  @POST
  @Path("/applyTemplates")
  @ApiOperation(value = "Gets complete yaml with templateRefs resolved", nickname = "getYamlWithTemplateRefsResolved")
  public ResponseDTO<String> applyTemplates(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      TemplateApplyRequestDTO templateApplyRequestDTO) {
    return null;
  }
}
