package io.harness.accesscontrol.roles.api;

import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.roles.api.RoleDTOMapper.fromDTO;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;

import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.roles.Role;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.events.RoleCreateEvent;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.outbox.api.OutboxService;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.Duration;
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
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import retrofit2.http.Body;

@Slf4j
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
public class RoleResource {
  private final RoleService roleService;
  private final ScopeService scopeService;
  private final RoleDTOMapper roleDTOMapper;
  private final TransactionTemplate transactionTemplate;
  private final OutboxService outboxService;

  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject
  public RoleResource(RoleService roleService, ScopeService scopeService, RoleDTOMapper roleDTOMapper,
      @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate, OutboxService outboxService) {
    this.roleService = roleService;
    this.scopeService = scopeService;
    this.roleDTOMapper = roleDTOMapper;
    this.transactionTemplate = transactionTemplate;
    this.outboxService = outboxService;
  }

  @GET
  @ApiOperation(value = "Get Roles", nickname = "getRoleList")
  public ResponseDTO<PageResponse<RoleResponseDTO>> get(@BeanParam PageRequest pageRequest,
      @BeanParam HarnessScopeParams harnessScopeParams,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    RoleFilter roleFilter =
        RoleFilter.builder().searchTerm(searchTerm).scopeIdentifier(scopeIdentifier).managedFilter(NO_FILTER).build();
    PageResponse<Role> pageResponse = roleService.list(pageRequest, roleFilter);
    return ResponseDTO.newResponse(pageResponse.map(roleDTOMapper::toResponseDTO));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get Role", nickname = "getRole")
  public ResponseDTO<RoleResponseDTO> get(@NotEmpty @PathParam(IDENTIFIER_KEY) String identifier,
      @BeanParam HarnessScopeParams harnessScopeParams, @QueryParam("harnessManaged") boolean isHarnessManaged) {
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
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
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    if (!identifier.equals(roleDTO.getIdentifier())) {
      throw new InvalidRequestException("Role identifier in the request body and the url do not match");
    }
    return ResponseDTO.newResponse(roleDTOMapper.toResponseDTO(roleService.update(fromDTO(scopeIdentifier, roleDTO))));
  }

  @POST
  @ApiOperation(value = "Create Role", nickname = "createRole")
  public ResponseDTO<RoleResponseDTO> create(@BeanParam HarnessScopeParams harnessScopeParams, @Body RoleDTO roleDTO) {
    Scope scope = scopeService.buildScopeFromParams(harnessScopeParams);
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
    String scopeIdentifier = scopeService.buildScopeFromParams(harnessScopeParams).toString();
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      RoleResponseDTO response = roleDTOMapper.toResponseDTO(roleService.delete(identifier, scopeIdentifier));
      outboxService.save(
          new RoleCreateEvent(response.getScope().getAccountIdentifier(), response.getRole(), response.getScope()));
      return ResponseDTO.newResponse(response);
    }));
  }
}
