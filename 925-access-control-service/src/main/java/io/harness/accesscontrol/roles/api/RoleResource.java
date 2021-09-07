package io.harness.accesscontrol.roles.api;

import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.accesscontrol.AccessControlPermissions.DELETE_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlPermissions.EDIT_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlPermissions.VIEW_ROLE_PERMISSION;
import static io.harness.accesscontrol.AccessControlResourceTypes.ROLE;
import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.roles.api.RoleDTOMapper.fromDTO;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.RoleUpdateResult;
import io.harness.accesscontrol.roles.events.RoleCreateEvent;
import io.harness.accesscontrol.roles.events.RoleDeleteEvent;
import io.harness.accesscontrol.roles.events.RoleUpdateEvent;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.outbox.api.OutboxService;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.transaction.support.TransactionTemplate;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("/roles")
@Path("/roles")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class RoleResource {
  private final RoleService roleService;
  private final ScopeService scopeService;
  private final RoleDTOMapper roleDTOMapper;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;
  private final AccessControlClient accessControlClient;

  private final RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;

  @Inject
  public RoleResource(RoleService roleService, ScopeService scopeService, RoleDTOMapper roleDTOMapper,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService,
      AccessControlClient accessControlClient) {
    this.roleService = roleService;
    this.scopeService = scopeService;
    this.roleDTOMapper = roleDTOMapper;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
    this.accessControlClient = accessControlClient;
  }

  @GET
  @ApiOperation(value = "Get Roles", nickname = "getRoleList")
  public ResponseDTO<PageResponse<RoleResponseDTO>> get(@BeanParam PageRequest pageRequest,
      @BeanParam HarnessScopeParams harnessScopeParams,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, null), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    RoleFilter roleFilter =
        RoleFilter.builder().searchTerm(searchTerm).scopeIdentifier(scopeIdentifier).managedFilter(NO_FILTER).build();
    PageResponse<Role> pageResponse = roleService.list(pageRequest, roleFilter);
    return ResponseDTO.newResponse(pageResponse.map(roleDTOMapper::toResponseDTO));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Role", nickname = "getRole")
  public ResponseDTO<RoleResponseDTO> get(
      @NotEmpty @PathParam(IDENTIFIER_KEY) String identifier, @BeanParam HarnessScopeParams harnessScopeParams) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, identifier), VIEW_ROLE_PERMISSION);
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    return ResponseDTO.newResponse(roleDTOMapper.toResponseDTO(
        roleService.get(identifier, scopeIdentifier, NO_FILTER).<InvalidRequestException>orElseThrow(() -> {
          throw new InvalidRequestException("Role not found with the given scope and identifier");
        })));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update Role", nickname = "updateRole")
  public ResponseDTO<RoleResponseDTO> update(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleDTO roleDTO) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, identifier), EDIT_ROLE_PERMISSION);
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    if (!identifier.equals(roleDTO.getIdentifier())) {
      throw new InvalidRequestException("Role identifier in the request body and the url do not match");
    }
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      RoleUpdateResult roleUpdateResult = roleService.update(fromDTO(scopeIdentifier, roleDTO));
      RoleResponseDTO response = roleDTOMapper.toResponseDTO(roleUpdateResult.getUpdatedRole());
      outboxService.save(new RoleUpdateEvent(response.getScope().getAccountIdentifier(), response.getRole(),
          roleDTOMapper.toResponseDTO(roleUpdateResult.getOriginalRole()).getRole(), response.getScope()));
      return ResponseDTO.newResponse(response);
    }));
  }

  @POST
  @ApiOperation(value = "Create Role", nickname = "createRole")
  public ResponseDTO<RoleResponseDTO> create(@BeanParam HarnessScopeParams harnessScopeParams, @Body RoleDTO roleDTO) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, null), EDIT_ROLE_PERMISSION);
    Scope scope = scopeService.getOrCreate(ScopeMapper.fromParams(harnessScopeParams));
    if (isEmpty(roleDTO.getAllowedScopeLevels())) {
      roleDTO.setAllowedScopeLevels(Sets.newHashSet(scope.getLevel().toString()));
    }
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      RoleResponseDTO response = roleDTOMapper.toResponseDTO(roleService.create(fromDTO(scope.toString(), roleDTO)));
      outboxService.save(
          new RoleCreateEvent(response.getScope().getAccountIdentifier(), response.getRole(), response.getScope()));
      return ResponseDTO.newResponse(response);
    }));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete Role", nickname = "deleteRole")
  public ResponseDTO<RoleResponseDTO> delete(
      @NotNull @PathParam(IDENTIFIER_KEY) String identifier, @BeanParam HarnessScopeParams harnessScopeParams) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
            harnessScopeParams.getProjectIdentifier()),
        Resource.of(ROLE, identifier), DELETE_ROLE_PERMISSION);
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      RoleResponseDTO response = roleDTOMapper.toResponseDTO(roleService.delete(identifier, scopeIdentifier));
      outboxService.save(
          new RoleDeleteEvent(response.getScope().getAccountIdentifier(), response.getRole(), response.getScope()));
      return ResponseDTO.newResponse(response);
    }));
  }
}
