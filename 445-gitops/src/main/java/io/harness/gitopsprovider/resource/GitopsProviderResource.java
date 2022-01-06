/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.gitopsprovider.resource;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.gitopsprovider.accesscontrol.Permissions.CREATE_PROJECT_PERMISSION;
import static io.harness.gitopsprovider.accesscontrol.Permissions.DELETE_PROJECT_PERMISSION;
import static io.harness.gitopsprovider.accesscontrol.Permissions.EDIT_PROJECT_PERMISSION;
import static io.harness.gitopsprovider.accesscontrol.Permissions.VIEW_PROJECT_PERMISSION;
import static io.harness.gitopsprovider.accesscontrol.ResourceTypes.PROJECT;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderResponseDTO;
import io.harness.gitopsprovider.entity.GitOpsProvider.GitOpsProviderKeys;
import io.harness.gitopsprovider.services.GitopsProviderService;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Api("/gitopsproviders")
@Path("/gitopsproviders")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })

@NextGenManagerAuth
@OwnedBy(HarnessTeam.GITOPS)
public class GitopsProviderResource {
  private final GitopsProviderService gitopsProviderService;
  private final AccessControlClient accessControlClient;

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get GitOps Provider", nickname = "getGitOpsProvider")
  public ResponseDTO<GitOpsProviderResponseDTO> get(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @OrgIdentifier @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @ProjectIdentifier @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier String projectIdentifier,
      @EntityIdentifier @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(PROJECT, identifier), VIEW_PROJECT_PERMISSION);
    Optional<GitOpsProviderResponseDTO> gitOpsProviderResponseDTO =
        gitopsProviderService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (!gitOpsProviderResponseDTO.isPresent()) {
      throw new NotFoundException(
          String.format("GitOpsProvider with identifier [%s] in project [%s], org [%s] not found", identifier,
              projectIdentifier, orgIdentifier));
    }
    return ResponseDTO.newResponse(gitOpsProviderResponseDTO.get());
  }

  @GET
  @Path("/list")
  @ApiOperation(value = "List GitOps Providers", nickname = "listGitOpsProviders")
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<PageResponse<GitOpsProviderResponseDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @QueryParam("sort") List<String> sort,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @OrgIdentifier @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @ProjectIdentifier @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @QueryParam("type") GitOpsProviderType type) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(PROJECT, null), VIEW_PROJECT_PERMISSION);
    Pageable pageRequest;
    if (EmptyPredicate.isEmpty(sort)) {
      pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, GitOpsProviderKeys.createdAt));
    } else {
      pageRequest = PageUtils.getPageRequest(page, size, sort);
    }
    return ResponseDTO.newResponse(getNGPageResponse(gitopsProviderService.list(
        pageRequest, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type)));
  }

  @POST
  @ApiOperation(value = "Creates a GitOpsProvider", nickname = "createGitOpsProvider")
  public ResponseDTO<GitOpsProviderResponseDTO> create(@Valid @NotNull GitOpsProviderDTO gitOpsProviderDTO,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(
          DEFAULT_ORG_IDENTIFIER) @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @ProjectIdentifier @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier String projectIdentifier,
      @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(PROJECT, null), CREATE_PROJECT_PERMISSION);
    return ResponseDTO.newResponse(gitopsProviderService.create(gitOpsProviderDTO, accountIdentifier));
  }

  @PUT
  @ApiOperation(value = "Updates a GitOpsProvider", nickname = "updateGitOpsProvider")
  public ResponseDTO<GitOpsProviderResponseDTO> update(@Valid @NotNull GitOpsProviderDTO gitopsProviderDTO,
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(
          DEFAULT_ORG_IDENTIFIER) @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @ProjectIdentifier @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier String projectIdentifier,
      @BeanParam GitEntityCreateInfoDTO gitEntityCreateInfo) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(PROJECT, null), EDIT_PROJECT_PERMISSION);
    return ResponseDTO.newResponse(gitopsProviderService.update(gitopsProviderDTO, accountIdentifier));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Deletes a GitOpsProvider", nickname = "deleteGitOpsProvider")
  @NGAccessControlCheck(resourceType = PROJECT, permission = DELETE_PROJECT_PERMISSION)
  public ResponseDTO<Boolean> delete(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @OrgIdentifier @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @ProjectIdentifier @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier String projectIdentifier,
      @EntityIdentifier @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(PROJECT, null), DELETE_PROJECT_PERMISSION);
    return ResponseDTO.newResponse(
        gitopsProviderService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier));
  }

  @GET
  @Path("validateUniqueIdentifier")
  @ApiOperation(value = "Validate Identifier is unique", nickname = "validateProviderIdentifierIsUnique")
  public ResponseDTO<Boolean> validateTheIdentifierIsUnique(
      @NotBlank @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) @EntityIdentifier String identifier) {
    return gitopsProviderService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier).isPresent()
        ? ResponseDTO.newResponse(false)
        : ResponseDTO.newResponse(true);
  }
}
