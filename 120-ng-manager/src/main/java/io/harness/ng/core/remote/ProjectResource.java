package io.harness.ng.core.remote;

import static io.harness.NGConstants.DEFAULT_ORG_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.accesscontrol.PlatformPermissions.CREATE_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.DELETE_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.EDIT_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_PROJECT_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.PROJECT;
import static io.harness.ng.core.remote.ProjectMapper.toResponseWrapper;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.ModuleType;
import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.dto.ProjectRequest;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.services.ProjectService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;

@OwnedBy(PL)
@Api("projects")
@Path("projects")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Tag(name = "Project", description = "This contains APIs related to Project as defined in Harness")
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
@NextGenManagerAuth
public class ProjectResource {
  private final ProjectService projectService;

  @POST
  @ApiOperation(value = "Create a Project", nickname = "postProject")
  @NGAccessControlCheck(resourceType = PROJECT, permission = CREATE_PROJECT_PERMISSION)
  @Operation(operationId = "postProject", summary = "Creates a Project",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns created project")
      })
  public ResponseDTO<ProjectResponse>
  create(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(
          description =
              "Organization identifier for the project. If left empty, the project will be create under default organization")
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(
          DEFAULT_ORG_IDENTIFIER) @OrgIdentifier String orgIdentifier,
      @RequestBody(required = true,
          description = "Details of the project to be created") @NotNull @Valid ProjectRequest projectDTO) {
    Project createdProject = projectService.create(accountIdentifier, orgIdentifier, projectDTO.getProject());
    return ResponseDTO.newResponse(createdProject.getVersion().toString(), toResponseWrapper(createdProject));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Gets a Project by identifier", nickname = "getProject")
  @Operation(operationId = "getProject", summary = "Gets a Project by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "default", description = "Returns project having projectIdentifier as specified in request")
      })
  @NGAccessControlCheck(resourceType = PROJECT, permission = VIEW_PROJECT_PERMISSION)
  public ResponseDTO<ProjectResponse>
  get(@NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(
          description =
              "Organization identifier for the project. If left empty, the project will be create under default organization")
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(
          DEFAULT_ORG_IDENTIFIER) @OrgIdentifier String orgIdentifier) {
    Optional<Project> projectOptional = projectService.get(accountIdentifier, orgIdentifier, identifier);
    if (!projectOptional.isPresent()) {
      throw new NotFoundException(
          String.format("Project with orgIdentifier [%s] and identifier [%s] not found", orgIdentifier, identifier));
    }
    return ResponseDTO.newResponse(
        projectOptional.get().getVersion().toString(), toResponseWrapper(projectOptional.get()));
  }

  @GET
  @ApiOperation(value = "Get Project list", nickname = "getProjectList")
  @Operation(operationId = "getProjectList", summary = "List user's project",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Paginated list of projects")
      })
  public ResponseDTO<PageResponse<ProjectResponse>>
  list(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam("hasModule") @DefaultValue("true") boolean hasModule,
      @Parameter(description = "list of projectIdentifiers to filter results by") @QueryParam(
          NGResourceFilterConstants.IDENTIFIERS) List<String> identifiers,
      @QueryParam(NGResourceFilterConstants.MODULE_TYPE_KEY) ModuleType moduleType,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(ProjectKeys.lastModifiedAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    ProjectFilterDTO projectFilterDTO =
        ProjectFilterDTO.builder()
            .searchTerm(searchTerm)
            .orgIdentifiers(StringUtils.isNotBlank(orgIdentifier) ? Collections.singleton(orgIdentifier) : null)
            .hasModule(hasModule)
            .moduleType(moduleType)
            .identifiers(identifiers)
            .build();
    Page<Project> projects =
        projectService.listPermittedProjects(accountIdentifier, getPageRequest(pageRequest), projectFilterDTO);
    return ResponseDTO.newResponse(getNGPageResponse(projects.map(ProjectMapper::toResponseWrapper)));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update a project by identifier", nickname = "putProject")
  @Operation(operationId = "putProject", summary = "Update project by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default", description = "updated project")
      })
  @NGAccessControlCheck(resourceType = PROJECT, permission = EDIT_PROJECT_PERMISSION)
  public ResponseDTO<ProjectResponse>
  update(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(
          description =
              "Organization identifier for the project. If left empty, the project will be create under default organization",
          required = false) @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(DEFAULT_ORG_IDENTIFIER)
      @OrgIdentifier String orgIdentifier,
      @RequestBody(required = true,
          description = "This is the updated project. This should have all the fields not just the updated ones")
      @NotNull @Valid ProjectRequest projectDTO) {
    projectDTO.getProject().setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    Project updatedProject =
        projectService.update(accountIdentifier, orgIdentifier, identifier, projectDTO.getProject());
    return ResponseDTO.newResponse(updatedProject.getVersion().toString(), toResponseWrapper(updatedProject));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a project by identifier", nickname = "deleteProject")
  @Operation(operationId = "deleteProject", summary = "Delete a project by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Boolean status whether request was successful or not")
      })
  @NGAccessControlCheck(resourceType = PROJECT, permission = DELETE_PROJECT_PERMISSION)
  public ResponseDTO<Boolean>
  delete(@HeaderParam(IF_MATCH) String ifMatch,
      @NotNull @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String identifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(
          description =
              "Organization identifier for the project. If left empty, the project will be create under default organization",
          required = false) @QueryParam(NGCommonEntityConstants.ORG_KEY) @DefaultValue(DEFAULT_ORG_IDENTIFIER)
      @OrgIdentifier String orgIdentifier) {
    return ResponseDTO.newResponse(projectService.delete(
        accountIdentifier, orgIdentifier, identifier, isNumeric(ifMatch) ? parseLong(ifMatch) : null));
  }
}
