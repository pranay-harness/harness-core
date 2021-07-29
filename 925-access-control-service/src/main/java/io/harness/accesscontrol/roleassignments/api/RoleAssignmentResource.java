package io.harness.accesscontrol.roleassignments.api;

import static io.harness.NGCommonEntityConstants.IDENTIFIER_KEY;
import static io.harness.accesscontrol.AccessControlPermissions.EDIT_SERVICEACCOUNT_PERMISSION;
import static io.harness.accesscontrol.AccessControlPermissions.MANAGE_USERGROUP_PERMISSION;
import static io.harness.accesscontrol.AccessControlPermissions.MANAGE_USER_PERMISSION;
import static io.harness.accesscontrol.common.filter.ManagedFilter.NO_FILTER;
import static io.harness.accesscontrol.principals.PrincipalType.SERVICE_ACCOUNT;
import static io.harness.accesscontrol.principals.PrincipalType.USER;
import static io.harness.accesscontrol.principals.PrincipalType.USER_GROUP;
import static io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO.MODEL_NAME;
import static io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTOMapper.fromDTO;
import static io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTOMapper.toDTO;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.USER_NOT_AUTHORIZED;
import static io.harness.outbox.TransactionOutboxModule.OUTBOX_TRANSACTION_TEMPLATE;
import static io.harness.springdata.TransactionUtils.DEFAULT_TRANSACTION_RETRY_POLICY;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import io.harness.accesscontrol.AccessControlPermissions;
import io.harness.accesscontrol.AccessControlResourceTypes;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.accesscontrol.common.validation.ValidationResult;
import io.harness.accesscontrol.commons.validation.HarnessActionValidator;
import io.harness.accesscontrol.principals.Principal;
import io.harness.accesscontrol.principals.PrincipalType;
import io.harness.accesscontrol.principals.serviceaccounts.HarnessServiceAccountService;
import io.harness.accesscontrol.principals.serviceaccounts.ServiceAccountService;
import io.harness.accesscontrol.principals.usergroups.HarnessUserGroupService;
import io.harness.accesscontrol.principals.usergroups.UserGroupService;
import io.harness.accesscontrol.principals.users.HarnessUserService;
import io.harness.accesscontrol.principals.users.UserService;
import io.harness.accesscontrol.resourcegroups.api.ResourceGroupDTO;
import io.harness.accesscontrol.resources.resourcegroups.HarnessResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.ResourceGroupService;
import io.harness.accesscontrol.resources.resourcegroups.api.ResourceGroupDTOMapper;
import io.harness.accesscontrol.roleassignments.RoleAssignment;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter;
import io.harness.accesscontrol.roleassignments.RoleAssignmentFilter.RoleAssignmentFilterBuilder;
import io.harness.accesscontrol.roleassignments.RoleAssignmentService;
import io.harness.accesscontrol.roleassignments.RoleAssignmentUpdateResult;
import io.harness.accesscontrol.roleassignments.events.RoleAssignmentCreateEvent;
import io.harness.accesscontrol.roleassignments.events.RoleAssignmentDeleteEvent;
import io.harness.accesscontrol.roleassignments.events.RoleAssignmentUpdateEvent;
import io.harness.accesscontrol.roles.RoleService;
import io.harness.accesscontrol.roles.api.RoleDTOMapper;
import io.harness.accesscontrol.roles.api.RoleResponseDTO;
import io.harness.accesscontrol.roles.filter.RoleFilter;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.harness.HarnessScopeParams;
import io.harness.accesscontrol.scopes.harness.HarnessScopeService;
import io.harness.accesscontrol.scopes.harness.ScopeMapper;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.outbox.api.OutboxService;
import io.harness.security.annotations.InternalApi;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.transaction.support.TransactionTemplate;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("roleassignments")
@Path("roleassignments")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@FieldDefaults(level = PRIVATE, makeFinal = true)
@Slf4j
public class RoleAssignmentResource {
  RoleAssignmentService roleAssignmentService;
  HarnessResourceGroupService harnessResourceGroupService;
  HarnessUserGroupService harnessUserGroupService;
  HarnessUserService harnessUserService;
  HarnessServiceAccountService harnessServiceAccountService;
  HarnessScopeService harnessScopeService;
  ScopeService scopeService;
  RoleService roleService;
  ResourceGroupService resourceGroupService;
  UserGroupService userGroupService;
  UserService userService;
  ServiceAccountService serviceAccountService;
  RoleAssignmentDTOMapper roleAssignmentDTOMapper;
  RoleDTOMapper roleDTOMapper;
  TransactionTemplate transactionTemplate;
  HarnessActionValidator<RoleAssignment> actionValidator;
  OutboxService outboxService;
  AccessControlClient accessControlClient;

  RetryPolicy<Object> transactionRetryPolicy = DEFAULT_TRANSACTION_RETRY_POLICY;

  @Inject
  public RoleAssignmentResource(RoleAssignmentService roleAssignmentService,
      HarnessResourceGroupService harnessResourceGroupService, HarnessUserGroupService harnessUserGroupService,
      HarnessUserService harnessUserService, HarnessServiceAccountService harnessServiceAccountService,
      HarnessScopeService harnessScopeService, ScopeService scopeService, RoleService roleService,
      ResourceGroupService resourceGroupService, UserGroupService userGroupService, UserService userService,
      ServiceAccountService serviceAccountService, RoleAssignmentDTOMapper roleAssignmentDTOMapper,
      RoleDTOMapper roleDTOMapper, @Named(OUTBOX_TRANSACTION_TEMPLATE) TransactionTemplate transactionTemplate,
      @Named(MODEL_NAME) HarnessActionValidator<RoleAssignment> actionValidator, OutboxService outboxService,
      AccessControlClient accessControlClient) {
    this.roleAssignmentService = roleAssignmentService;
    this.harnessResourceGroupService = harnessResourceGroupService;
    this.harnessUserGroupService = harnessUserGroupService;
    this.harnessUserService = harnessUserService;
    this.harnessServiceAccountService = harnessServiceAccountService;
    this.harnessScopeService = harnessScopeService;
    this.scopeService = scopeService;
    this.roleService = roleService;
    this.resourceGroupService = resourceGroupService;
    this.userGroupService = userGroupService;
    this.userService = userService;
    this.serviceAccountService = serviceAccountService;
    this.roleAssignmentDTOMapper = roleAssignmentDTOMapper;
    this.roleDTOMapper = roleDTOMapper;
    this.transactionTemplate = transactionTemplate;
    this.actionValidator = actionValidator;
    this.outboxService = outboxService;
    this.accessControlClient = accessControlClient;
  }

  @GET
  @ApiOperation(value = "Get Role Assignments", nickname = "getRoleAssignmentList")
  public ResponseDTO<PageResponse<RoleAssignmentResponseDTO>> get(
      @BeanParam PageRequest pageRequest, @BeanParam HarnessScopeParams harnessScopeParams) {
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    RoleAssignmentFilterBuilder roleAssignmentFilterBuilder =
        RoleAssignmentFilter.builder().scopeFilter(scopeIdentifier);
    Set<PrincipalType> principalTypes = Sets.newHashSet();

    if (checkViewPermission(harnessScopeParams, USER)) {
      principalTypes.add(USER);
    }

    if (checkViewPermission(harnessScopeParams, USER_GROUP)) {
      principalTypes.add(USER_GROUP);
    }

    if (checkViewPermission(harnessScopeParams, SERVICE_ACCOUNT)) {
      principalTypes.add(SERVICE_ACCOUNT);
    }

    if (principalTypes.isEmpty()) {
      throw new UnauthorizedException("Current principal is not authorized to the view the role assignments",
          USER_NOT_AUTHORIZED, WingsException.USER);
    }

    PageResponse<RoleAssignment> pageResponse = roleAssignmentService.list(
        pageRequest, roleAssignmentFilterBuilder.principalTypeFilter(principalTypes).build());
    return ResponseDTO.newResponse(pageResponse.map(roleAssignmentDTOMapper::toResponseDTO));
  }

  @POST
  @Path("filter")
  @ApiOperation(value = "Get Filtered Role Assignments", nickname = "getFilteredRoleAssignmentList")
  public ResponseDTO<PageResponse<RoleAssignmentResponseDTO>> get(@BeanParam PageRequest pageRequest,
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleAssignmentFilterDTO roleAssignmentFilter) {
    Optional<RoleAssignmentFilter> filter =
        buildRoleAssignmentFilterWithPermissionFilter(harnessScopeParams, roleAssignmentFilter);
    if (!filter.isPresent()) {
      throw new UnauthorizedException("Current principal is not authorized to the view the role assignments",
          USER_NOT_AUTHORIZED, WingsException.USER);
    }
    PageResponse<RoleAssignment> pageResponse = roleAssignmentService.list(pageRequest, filter.get());
    return ResponseDTO.newResponse(pageResponse.map(roleAssignmentDTOMapper::toResponseDTO));
  }

  @POST
  @Path("aggregate")
  @ApiOperation(value = "Get Role Assignments Aggregate", nickname = "getRoleAssignmentsAggregate")
  public ResponseDTO<RoleAssignmentAggregateResponseDTO> getAggregated(
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleAssignmentFilterDTO roleAssignmentFilter) {
    Scope scope = ScopeMapper.fromParams(harnessScopeParams);
    Optional<RoleAssignmentFilter> filter =
        buildRoleAssignmentFilterWithPermissionFilter(harnessScopeParams, roleAssignmentFilter);
    if (!filter.isPresent()) {
      throw new UnauthorizedException("Current principal is not authorized to the view the role assignments",
          USER_NOT_AUTHORIZED, WingsException.USER);
    }
    PageRequest pageRequest = PageRequest.builder().pageSize(1000).build();
    List<RoleAssignment> roleAssignments = roleAssignmentService.list(pageRequest, filter.get()).getContent();
    List<String> roleIdentifiers =
        roleAssignments.stream().map(RoleAssignment::getRoleIdentifier).distinct().collect(toList());
    RoleFilter roleFilter = RoleFilter.builder()
                                .identifierFilter(new HashSet<>(roleIdentifiers))
                                .scopeIdentifier(scope.toString())
                                .managedFilter(NO_FILTER)
                                .build();
    List<RoleResponseDTO> roleResponseDTOs = roleService.list(pageRequest, roleFilter)
                                                 .getContent()
                                                 .stream()
                                                 .map(roleDTOMapper::toResponseDTO)
                                                 .collect(toList());
    List<String> resourceGroupIdentifiers =
        roleAssignments.stream().map(RoleAssignment::getResourceGroupIdentifier).distinct().collect(toList());
    List<ResourceGroupDTO> resourceGroupDTOs = resourceGroupService.list(resourceGroupIdentifiers, scope.toString())
                                                   .stream()
                                                   .map(ResourceGroupDTOMapper::toDTO)
                                                   .collect(toList());
    List<RoleAssignmentDTO> roleAssignmentDTOs =
        roleAssignments.stream().map(RoleAssignmentDTOMapper::toDTO).collect(toList());
    return ResponseDTO.newResponse(
        RoleAssignmentAggregateResponseDTOMapper.toDTO(roleAssignmentDTOs, scope, roleResponseDTOs, resourceGroupDTOs));
  }

  @POST
  @ApiOperation(value = "Create Role Assignment", nickname = "createRoleAssignment")
  public ResponseDTO<RoleAssignmentResponseDTO> create(
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleAssignmentDTO roleAssignmentDTO) {
    Scope scope = ScopeMapper.fromParams(harnessScopeParams);
    RoleAssignment roleAssignment = fromDTO(scope, roleAssignmentDTO);
    syncDependencies(roleAssignment, scope);
    checkUpdatePermission(harnessScopeParams, roleAssignment);
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      RoleAssignment createdRoleAssignment = roleAssignmentService.create(roleAssignment);
      RoleAssignmentResponseDTO response = roleAssignmentDTOMapper.toResponseDTO(createdRoleAssignment);
      outboxService.save(new RoleAssignmentCreateEvent(
          response.getScope().getAccountIdentifier(), response.getRoleAssignment(), response.getScope()));
      return ResponseDTO.newResponse(response);
    }));
  }

  @PUT
  @Path("{identifier}")
  @ApiOperation(value = "Update Role Assignment", nickname = "updateRoleAssignment")
  public ResponseDTO<RoleAssignmentResponseDTO> update(@NotNull @PathParam(IDENTIFIER_KEY) String identifier,
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleAssignmentDTO roleAssignmentDTO) {
    Scope scope = ScopeMapper.fromParams(harnessScopeParams);
    if (!identifier.equals(roleAssignmentDTO.getIdentifier())) {
      throw new InvalidRequestException("Role Assignment identifier in the request body and the url do not match.");
    }
    RoleAssignment roleAssignmentUpdate = fromDTO(scope, roleAssignmentDTO);
    checkUpdatePermission(harnessScopeParams, roleAssignmentUpdate);
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      RoleAssignmentUpdateResult roleAssignmentUpdateResult = roleAssignmentService.update(roleAssignmentUpdate);
      RoleAssignmentResponseDTO response =
          roleAssignmentDTOMapper.toResponseDTO(roleAssignmentUpdateResult.getUpdatedRoleAssignment());
      outboxService.save(
          new RoleAssignmentUpdateEvent(response.getScope().getAccountIdentifier(), response.getRoleAssignment(),
              roleAssignmentDTOMapper.toResponseDTO(roleAssignmentUpdateResult.getOriginalRoleAssignment())
                  .getRoleAssignment(),
              response.getScope()));
      return ResponseDTO.newResponse(response);
    }));
  }

  private List<RoleAssignmentResponseDTO> createRoleAssignments(
      HarnessScopeParams harnessScopeParams, RoleAssignmentCreateRequestDTO requestDTO, boolean managed) {
    Scope scope = ScopeMapper.fromParams(harnessScopeParams);
    List<RoleAssignment> roleAssignmentsPayload =
        requestDTO.getRoleAssignments()
            .stream()
            .map(roleAssignmentDTO -> fromDTO(scope, roleAssignmentDTO, managed))
            .collect(Collectors.toList());
    List<RoleAssignmentResponseDTO> createdRoleAssignments = new ArrayList<>();
    for (RoleAssignment roleAssignment : roleAssignmentsPayload) {
      try {
        syncDependencies(roleAssignment, scope);
        checkUpdatePermission(harnessScopeParams, roleAssignment);
        RoleAssignmentResponseDTO roleAssignmentResponseDTO =
            Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
              RoleAssignmentResponseDTO response =
                  roleAssignmentDTOMapper.toResponseDTO(roleAssignmentService.create(roleAssignment));
              outboxService.save(new RoleAssignmentCreateEvent(
                  response.getScope().getAccountIdentifier(), response.getRoleAssignment(), response.getScope()));
              return response;
            }));
        createdRoleAssignments.add(roleAssignmentResponseDTO);
      } catch (Exception e) {
        log.error(String.format("Could not create role assignment %s", roleAssignment), e);
      }
    }
    return createdRoleAssignments;
  }
  /**
   * idempotent call, calling it multiple times won't create any side effect,
   * returns all role assignments which were created ignoring duplicates or failures, if any.
   */
  @POST
  @Path("/multi")
  @ApiOperation(value = "Create Multiple Role Assignments", nickname = "createRoleAssignments")
  public ResponseDTO<List<RoleAssignmentResponseDTO>> create(@BeanParam HarnessScopeParams harnessScopeParams,
      @Body RoleAssignmentCreateRequestDTO roleAssignmentCreateRequestDTO) {
    return ResponseDTO.newResponse(createRoleAssignments(harnessScopeParams, roleAssignmentCreateRequestDTO, false));
  }

  @POST
  @Path("/multi/internal")
  @InternalApi
  @ApiOperation(value = "Create Multiple Role Assignments", nickname = "createRoleAssignmentsInternal")
  public ResponseDTO<List<RoleAssignmentResponseDTO>> create(@BeanParam HarnessScopeParams harnessScopeParams,
      @Body RoleAssignmentCreateRequestDTO roleAssignmentCreateRequestDTO,
      @QueryParam("managed") @DefaultValue("false") Boolean managed) {
    return ResponseDTO.newResponse(createRoleAssignments(harnessScopeParams, roleAssignmentCreateRequestDTO, managed));
  }

  @POST
  @Path("/validate")
  @ApiOperation(value = "Validate Role Assignment", nickname = "validateRoleAssignment")
  public ResponseDTO<RoleAssignmentValidationResponseDTO> validate(
      @BeanParam HarnessScopeParams harnessScopeParams, @Body RoleAssignmentValidationRequestDTO validationRequest) {
    Scope scope = ScopeMapper.fromParams(harnessScopeParams);
    harnessResourceGroupService.sync(validationRequest.getRoleAssignment().getResourceGroupIdentifier(), scope);
    return ResponseDTO.newResponse(toDTO(roleAssignmentService.validate(fromDTO(scope, validationRequest))));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete Role Assignment", nickname = "deleteRoleAssignment")
  public ResponseDTO<RoleAssignmentResponseDTO> delete(
      @BeanParam HarnessScopeParams harnessScopeParams, @NotEmpty @PathParam(IDENTIFIER_KEY) String identifier) {
    String scopeIdentifier = ScopeMapper.fromParams(harnessScopeParams).toString();
    RoleAssignment roleAssignment =
        roleAssignmentService.get(identifier, scopeIdentifier).<InvalidRequestException>orElseThrow(() -> {
          throw new InvalidRequestException("Invalid Role Assignment");
        });
    checkUpdatePermission(harnessScopeParams, roleAssignment);
    ValidationResult validationResult = actionValidator.canDelete(roleAssignment);
    if (!validationResult.isValid()) {
      throw new InvalidRequestException(validationResult.getErrorMessage());
    }
    return Failsafe.with(transactionRetryPolicy).get(() -> transactionTemplate.execute(status -> {
      RoleAssignment deletedRoleAssignment =
          roleAssignmentService.delete(identifier, scopeIdentifier).<NotFoundException>orElseThrow(() -> {
            throw new NotFoundException("Role Assignment is already deleted");
          });
      RoleAssignmentResponseDTO response = roleAssignmentDTOMapper.toResponseDTO(deletedRoleAssignment);
      outboxService.save(new RoleAssignmentDeleteEvent(
          response.getScope().getAccountIdentifier(), response.getRoleAssignment(), response.getScope()));
      return ResponseDTO.newResponse(response);
    }));
  }

  private void checkUpdatePermission(HarnessScopeParams harnessScopeParams, RoleAssignment roleAssignment) {
    if (USER_GROUP.equals(roleAssignment.getPrincipalType())) {
      accessControlClient.checkForAccessOrThrow(
          ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
              harnessScopeParams.getProjectIdentifier()),
          Resource.of(AccessControlResourceTypes.USER_GROUP, roleAssignment.getPrincipalIdentifier()),
          MANAGE_USERGROUP_PERMISSION);
    } else if (USER.equals(roleAssignment.getPrincipalType())) {
      accessControlClient.checkForAccessOrThrow(
          ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
              harnessScopeParams.getProjectIdentifier()),
          Resource.of(AccessControlResourceTypes.USER, roleAssignment.getPrincipalIdentifier()),
          MANAGE_USER_PERMISSION);
    } else if (SERVICE_ACCOUNT.equals(roleAssignment.getPrincipalType())) {
      accessControlClient.checkForAccessOrThrow(
          ResourceScope.of(harnessScopeParams.getAccountIdentifier(), harnessScopeParams.getOrgIdentifier(),
              harnessScopeParams.getProjectIdentifier()),
          Resource.of(AccessControlResourceTypes.SERVICEACCOUNT, roleAssignment.getPrincipalIdentifier()),
          EDIT_SERVICEACCOUNT_PERMISSION);
    } else {
      throw new InvalidRequestException(String.format(
          "Role assignments for principalType %s cannot be changed", roleAssignment.getPrincipalType().toString()));
    }
  }

  private boolean checkViewPermission(HarnessScopeParams harnessScopeParams, PrincipalType principalType) {
    String resourceType = null;
    String permissionIdentifier = null;
    if (USER.equals(principalType)) {
      resourceType = AccessControlResourceTypes.USER;
      permissionIdentifier = AccessControlPermissions.VIEW_USER_PERMISSION;
    } else if (USER_GROUP.equals(principalType)) {
      resourceType = AccessControlResourceTypes.USER_GROUP;
      permissionIdentifier = AccessControlPermissions.VIEW_USERGROUP_PERMISSION;
    } else if (SERVICE_ACCOUNT.equals(principalType)) {
      resourceType = AccessControlResourceTypes.SERVICEACCOUNT;
      permissionIdentifier = AccessControlPermissions.VIEW_SERVICEACCOUNT_PERMISSION;
    } else {
      throw new InvalidRequestException("Invalid Principal type: " + principalType.toString());
    }
    return accessControlClient.hasAccess(ResourceScope.builder()
                                             .projectIdentifier(harnessScopeParams.getProjectIdentifier())
                                             .orgIdentifier(harnessScopeParams.getOrgIdentifier())
                                             .accountIdentifier(harnessScopeParams.getAccountIdentifier())
                                             .build(),
        Resource.of(resourceType, null), permissionIdentifier);
  }

  private Optional<RoleAssignmentFilter> buildRoleAssignmentFilterWithPermissionFilter(
      HarnessScopeParams harnessScopeParams, RoleAssignmentFilterDTO roleAssignmentFilterDTO) {
    boolean hasAccessToUserRoleAssignments = checkViewPermission(harnessScopeParams, USER);
    boolean hasAccessToUserGroupRoleAssignments = checkViewPermission(harnessScopeParams, USER_GROUP);
    boolean hasAccessToServiceAccountRoleAssignments = checkViewPermission(harnessScopeParams, SERVICE_ACCOUNT);
    Scope scope = ScopeMapper.fromParams(harnessScopeParams);
    RoleAssignmentFilter roleAssignmentFilter = fromDTO(scope.toString(), roleAssignmentFilterDTO);
    if (isNotEmpty(roleAssignmentFilter.getPrincipalFilter())) {
      Set<Principal> principals = roleAssignmentFilter.getPrincipalFilter();
      if (!hasAccessToUserGroupRoleAssignments) {
        principals = principals.stream()
                         .filter(principal -> !USER_GROUP.equals(principal.getPrincipalType()))
                         .collect(Collectors.toSet());
      }
      if (!hasAccessToUserRoleAssignments) {
        principals = principals.stream()
                         .filter(principal -> !USER.equals(principal.getPrincipalType()))
                         .collect(Collectors.toSet());
      }
      if (!hasAccessToServiceAccountRoleAssignments) {
        principals = principals.stream()
                         .filter(principal -> !SERVICE_ACCOUNT.equals(principal.getPrincipalType()))
                         .collect(Collectors.toSet());
      }
      if (isEmpty(principals)) {
        return Optional.empty();
      }
      roleAssignmentFilter.setPrincipalFilter(principals);
    } else if (isNotEmpty(roleAssignmentFilter.getPrincipalTypeFilter())) {
      if (!hasAccessToUserGroupRoleAssignments) {
        roleAssignmentFilter.getPrincipalTypeFilter().remove(USER_GROUP);
      }
      if (!hasAccessToUserRoleAssignments) {
        roleAssignmentFilter.getPrincipalTypeFilter().remove(USER);
      }
      if (!hasAccessToServiceAccountRoleAssignments) {
        roleAssignmentFilter.getPrincipalTypeFilter().remove(SERVICE_ACCOUNT);
      }
      if (isEmpty(roleAssignmentFilter.getPrincipalTypeFilter())) {
        return Optional.empty();
      }
    } else {
      Set<PrincipalType> principalTypes = Sets.newHashSet();
      if (checkViewPermission(harnessScopeParams, USER)) {
        principalTypes.add(USER);
      }

      if (checkViewPermission(harnessScopeParams, USER_GROUP)) {
        principalTypes.add(USER_GROUP);
      }

      if (checkViewPermission(harnessScopeParams, SERVICE_ACCOUNT)) {
        principalTypes.add(SERVICE_ACCOUNT);
      }

      if (principalTypes.isEmpty()) {
        return Optional.empty();
      } else {
        roleAssignmentFilter.setPrincipalTypeFilter(principalTypes);
      }
    }
    return Optional.of(roleAssignmentFilter);
  }

  private void syncDependencies(RoleAssignment roleAssignment, Scope scope) {
    if (!scopeService.isPresent(scope.toString())) {
      harnessScopeService.sync(scope);
    }
    if (!resourceGroupService.get(roleAssignment.getResourceGroupIdentifier(), scope.toString()).isPresent()) {
      harnessResourceGroupService.sync(roleAssignment.getResourceGroupIdentifier(), scope);
    }
    if (roleAssignment.getPrincipalType().equals(USER_GROUP)
        && !userGroupService.get(roleAssignment.getPrincipalIdentifier(), scope.toString()).isPresent()) {
      harnessUserGroupService.sync(roleAssignment.getPrincipalIdentifier(), scope);
    }
    if (roleAssignment.getPrincipalType().equals(USER)
        && !userService.get(roleAssignment.getPrincipalIdentifier(), scope.toString()).isPresent()) {
      harnessUserService.sync(roleAssignment.getPrincipalIdentifier(), scope);
    }
    if (roleAssignment.getPrincipalType().equals(SERVICE_ACCOUNT)
        && !serviceAccountService.get(roleAssignment.getPrincipalIdentifier(), scope.toString()).isPresent()) {
      harnessServiceAccountService.sync(roleAssignment.getPrincipalIdentifier(), scope);
    }
  }
}
