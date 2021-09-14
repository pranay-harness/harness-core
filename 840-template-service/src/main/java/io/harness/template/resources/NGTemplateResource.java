/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.gitsync.interceptor.GitEntityDeleteInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityUpdateInfoDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.template.beans.TemplateApplyRequestDTO;
import io.harness.template.beans.TemplateFilterPropertiesDTO;
import io.harness.template.beans.TemplateResponseDTO;
import io.harness.template.beans.TemplateSummaryResponseDTO;
import io.harness.template.entity.TemplateEntity;
import io.harness.template.entity.TemplateEntity.TemplateEntityKeys;
import io.harness.template.mappers.NGTemplateDtoMapper;
import io.harness.template.services.NGTemplateService;
import io.harness.template.services.NGTemplateServiceHelper;
import io.harness.utils.PageUtils;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import retrofit2.http.Body;

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
  private final NGTemplateServiceHelper templateServiceHelper;

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
                "Template with the given Identifier: %s and %s does not exist or has been deleted", templateIdentifier,
                EmptyPredicate.isEmpty(versionLabel) ? "stable versionLabel" : "versionLabel: " + versionLabel))));
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
  @Path("/updateStableTemplate/{templateIdentifier}/{versionLabel}")
  @ApiOperation(value = "Updating stable template label", nickname = "updateStableTemplate")
  public ResponseDTO<String> updateStableTemplate(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam("templateIdentifier") @ResourceIdentifier String templateIdentifier,
      @PathParam(NGCommonEntityConstants.VERSION_LABEL_KEY) String versionLabel,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    log.info(String.format(
        "Updating Stable Template with identifier %s with versionLabel %s in project %s, org %s, account %s",
        templateIdentifier, versionLabel, projectId, orgId, accountId));

    TemplateEntity templateEntity = templateService.updateStableTemplateVersion(
        accountId, orgId, projectId, templateIdentifier, versionLabel, gitEntityBasicInfo);
    return ResponseDTO.newResponse(templateEntity.getVersion().toString(), templateEntity.getVersionLabel());
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
  @Path("/{templateIdentifier}/{versionLabel}")
  @ApiOperation(value = "Deletes template versionLabel", nickname = "deleteTemplateLabel")
  public ResponseDTO<Boolean> deleteTemplate(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @PathParam("templateIdentifier") @ResourceIdentifier String templateIdentifier,
      @NotNull @PathParam(NGCommonEntityConstants.VERSION_LABEL_KEY) String versionLabel,
      @BeanParam GitEntityDeleteInfoDTO entityDeleteInfo) {
    log.info(String.format("Deleting Template with identifier %s and versionLabel %s in project %s, org %s, account %s",
        templateIdentifier, versionLabel, projectId, orgId, accountId));

    return ResponseDTO.newResponse(templateService.delete(
        accountId, orgId, projectId, templateIdentifier, versionLabel, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }

  @POST
  @Path("/list")
  @ApiOperation(value = "Gets all template list", nickname = "getTemplateList")
  // will return non deleted templates only
  public ResponseDTO<Page<TemplateSummaryResponseDTO>> listTemplates(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgId,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectId,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("25") int size,
      @QueryParam("sort") List<String> sort, @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("filterIdentifier") String filterIdentifier, @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo,
      @Body TemplateFilterPropertiesDTO filterProperties,
      @QueryParam("getDistinctFromBranches") Boolean getDistinctFromBranches) {
    log.info(String.format("Get List of templates in project: %s, org: %s, account: %s", projectId, orgId, accountId));
    Criteria criteria = templateServiceHelper.formCriteria(
        accountId, orgId, projectId, filterIdentifier, filterProperties, false, searchTerm);

    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, TemplateEntityKeys.lastUpdatedAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }

    Page<TemplateSummaryResponseDTO> templateSummaryResponseDTOS =
        templateService.list(criteria, pageRequest, accountId, orgId, projectId, getDistinctFromBranches)
            .map(NGTemplateDtoMapper::prepareTemplateSummaryResponseDto);
    return ResponseDTO.newResponse(templateSummaryResponseDTOS);
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
