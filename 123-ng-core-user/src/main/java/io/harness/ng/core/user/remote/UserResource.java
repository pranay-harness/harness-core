package io.harness.ng.core.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ng.accesscontrol.PlatformPermissions.MANAGE_USER_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_USER_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.USER;
import static io.harness.utils.PageUtils.getPageRequest;

import static java.lang.Boolean.TRUE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.clients.Resource;
import io.harness.accesscontrol.clients.ResourceScope;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.accesscontrol.user.ACLAggregateFilter;
import io.harness.ng.accesscontrol.user.AggregateUserService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.AddUsersDTO;
import io.harness.ng.core.user.AddUsersResponse;
import io.harness.ng.core.user.PasswordChangeDTO;
import io.harness.ng.core.user.PasswordChangeResponse;
import io.harness.ng.core.user.TwoFactorAuthMechanismInfo;
import io.harness.ng.core.user.TwoFactorAuthSettingsInfo;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserMembershipUpdateSource;
import io.harness.ng.core.user.remote.dto.UserAggregateDTO;
import io.harness.ng.core.user.remote.dto.UserFilter;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.ng.core.user.remote.mapper.UserMetadataMapper;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.ng.userprofile.services.api.UserInfoService;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.annotations.InternalApi;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.dto.PrincipalType;
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
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import retrofit2.http.Body;

@Api("user")
@Path("user")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@Slf4j
@OwnedBy(PL)
public class UserResource {
  AggregateUserService aggregateUserService;
  NgUserService ngUserService;
  ProjectService projectService;
  UserInfoService userInfoService;
  AccessControlClient accessControlClient;

  @GET
  @Path("currentUser")
  @ApiOperation(value = "get current user information", nickname = "getCurrentUserInfo")
  public ResponseDTO<UserInfo> getUserInfo() {
    return ResponseDTO.newResponse(userInfoService.getCurrentUser());
  }

  @GET
  @Path("two-factor-auth/{authMechanism}")
  @ApiOperation(value = "get two factor auth settings", nickname = "getTwoFactorAuthSettings")
  public ResponseDTO<TwoFactorAuthSettingsInfo> getTwoFactorAuthSettingsInfo(
      @PathParam("authMechanism") TwoFactorAuthMechanismInfo authMechanism) {
    return ResponseDTO.newResponse(userInfoService.getTwoFactorAuthSettingsInfo(authMechanism));
  }

  @GET
  @Path("usermembership")
  @ApiOperation(value = "Check if user part of scope", nickname = "checkUserMembership", hidden = true)
  @InternalApi
  public ResponseDTO<Boolean> checkUserMembership(@QueryParam(NGCommonEntityConstants.USER_ID) String userId,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USER, null), VIEW_USER_PERMISSION);
    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    return ResponseDTO.newResponse(ngUserService.isUserAtScope(userId, scope));
  }

  @GET
  @Path("currentgen")
  @ApiOperation(value = "Get users from current gen for an account", nickname = "getCurrentGenUsers")
  public ResponseDTO<PageResponse<UserMetadataDTO>> getCurrentGenUsers(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("searchString") @DefaultValue("") String searchString, @BeanParam PageRequest pageRequest) {
    accessControlClient.checkForAccessOrThrow(
        ResourceScope.of(accountIdentifier, null, null), Resource.of(USER, null), VIEW_USER_PERMISSION);
    Pageable pageable = getPageRequest(pageRequest);
    Page<UserInfo> users = ngUserService.listCurrentGenUsers(accountIdentifier, searchString, pageable);
    return ResponseDTO.newResponse(PageUtils.getNGPageResponse(users.map(UserMetadataMapper::toDTO)));
  }

  @GET
  @Path("projects")
  @ApiOperation(value = "get user project information", nickname = "getUserProjectInfo")
  public ResponseDTO<PageResponse<ProjectDTO>> getUserProjectInfo(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest pageRequest) {
    Optional<String> userId = getUserIdentifierFromSecurityContext();
    if (!userId.isPresent()) {
      return ResponseDTO.newResponse(PageResponse.getEmptyPageResponse(pageRequest));
    }
    return ResponseDTO.newResponse(projectService.listProjectsForUser(userId.get(), accountId, pageRequest));
  }

  @GET
  @Path("all-projects")
  @ApiOperation(value = "get user all projects information", nickname = "getUserAllProjectsInfo")
  public ResponseDTO<List<ProjectDTO>> getUserAllProjectsInfo(
      @QueryParam("accountId") String accountId, @QueryParam("userId") String userId) {
    return ResponseDTO.newResponse(projectService.listProjectsForUser(userId, accountId));
  }

  @POST
  @Path("batch")
  @ApiOperation(value = "Get a list of users", nickname = "getUsers")
  public ResponseDTO<PageResponse<UserMetadataDTO>> getUsers(
      @NotNull @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Valid @BeanParam PageRequest pageRequest, UserFilter userFilter) {
    if (userFilter == null || UserFilter.ParentFilter.NO_PARENT_SCOPES.equals(userFilter.getParentFilter())) {
      accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
          Resource.of(USER, null), VIEW_USER_PERMISSION);
    } else {
      accessControlClient.checkForAccessOrThrow(
          ResourceScope.of(accountIdentifier, null, null), Resource.of(USER, null), VIEW_USER_PERMISSION);
      if (isNotEmpty(orgIdentifier)) {
        accessControlClient.checkForAccessOrThrow(
            ResourceScope.of(accountIdentifier, orgIdentifier, null), Resource.of(USER, null), VIEW_USER_PERMISSION);
        if (isNotEmpty(projectIdentifier)) {
          accessControlClient.checkForAccessOrThrow(
              ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier), Resource.of(USER, null),
              VIEW_USER_PERMISSION);
        }
      }
    }
    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    return ResponseDTO.newResponse(ngUserService.listUsers(scope, pageRequest, userFilter));
  }

  @GET
  @Path("/aggregate/{userId}")
  @ApiOperation(value = "Get a user by userId for access control", nickname = "getAggregatedUser")
  public ResponseDTO<UserAggregateDTO> getAggregatedUser(@PathParam("userId") String userId,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USER, userId), VIEW_USER_PERMISSION);

    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    UserAggregateDTO aclUserAggregateDTOs = aggregateUserService.getAggregatedUser(scope, userId);
    return ResponseDTO.newResponse(aclUserAggregateDTOs);
  }

  @POST
  @Path("aggregate")
  @ApiOperation(value = "Get a page of active users for access control", nickname = "getAggregatedUsers")
  public ResponseDTO<PageResponse<UserAggregateDTO>> getAggregatedUsers(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam("searchTerm") String searchTerm, @BeanParam PageRequest pageRequest,
      ACLAggregateFilter aclAggregateFilter) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USER, null), VIEW_USER_PERMISSION);

    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    if (ACLAggregateFilter.isFilterApplied(aclAggregateFilter) && isNotBlank(searchTerm)) {
      throw new InvalidRequestException("Search and Filter are not supported together");
    }
    if (ACLAggregateFilter.isFilterApplied(aclAggregateFilter)) {
      return ResponseDTO.newResponse(aggregateUserService.getAggregatedUsers(scope, aclAggregateFilter, pageRequest));
    }
    return ResponseDTO.newResponse(aggregateUserService.getAggregatedUsers(scope, searchTerm, pageRequest));
  }

  @POST
  @Path("users")
  @ApiOperation(value = "Add users to a scope", nickname = "addUsers")
  public ResponseDTO<AddUsersResponse> addUsers(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @Valid AddUsersDTO addUsersDTO) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USER, null), MANAGE_USER_PERMISSION);
    return ResponseDTO.newResponse(
        ngUserService.addUsers(Scope.of(accountIdentifier, orgIdentifier, projectIdentifier), addUsersDTO));
  }

  @PUT
  @ApiOperation(value = "update user information", nickname = "updateUserInfo")
  public ResponseDTO<UserInfo> updateUserInfo(@Body UserInfo userInfo) {
    return ResponseDTO.newResponse(userInfoService.update(userInfo));
  }

  @PUT
  @Path("password")
  @ApiOperation(value = "Change user password", nickname = "changeUserPassword")
  public ResponseDTO<PasswordChangeResponse> changeUserPassword(PasswordChangeDTO passwordChangeDTO) {
    return ResponseDTO.newResponse(userInfoService.changeUserPassword(passwordChangeDTO));
  }

  @PUT
  @Path("enable-two-factor-auth")
  @ApiOperation(value = "enable two factor auth settings", nickname = "enableTwoFactorAuth")
  public ResponseDTO<UserInfo> updateTwoFactorAuthInfo(@Body TwoFactorAuthSettingsInfo authSettingsInfo) {
    return ResponseDTO.newResponse(userInfoService.updateTwoFactorAuthInfo(authSettingsInfo));
  }

  @PUT
  @Path("disable-two-factor-auth")
  @ApiOperation(value = "disable two factor auth settings", nickname = "disableTwoFactorAuth")
  public ResponseDTO<UserInfo> disableTFA() {
    return ResponseDTO.newResponse(userInfoService.disableTFA());
  }

  @DELETE
  @Path("{userId}")
  @Produces("application/json")
  @Consumes()
  @ApiOperation(value = "Remove user as the collaborator from the scope", nickname = "removeUser")
  public ResponseDTO<Boolean> removeUser(@NotNull @PathParam("userId") String userId,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USER, userId), MANAGE_USER_PERMISSION);
    Scope scope = Scope.builder()
                      .accountIdentifier(accountIdentifier)
                      .orgIdentifier(orgIdentifier)
                      .projectIdentifier(projectIdentifier)
                      .build();
    return ResponseDTO.newResponse(
        TRUE.equals(ngUserService.removeUserFromScope(userId, scope, UserMembershipUpdateSource.USER)));
  }

  public Optional<String> getUserIdentifierFromSecurityContext() {
    Optional<String> userId = Optional.empty();
    if (SourcePrincipalContextBuilder.getSourcePrincipal() != null
        && SourcePrincipalContextBuilder.getSourcePrincipal().getType() == PrincipalType.USER) {
      userId = Optional.of(SourcePrincipalContextBuilder.getSourcePrincipal().getName());
    }
    return userId;
  }

  @PUT
  @Path("unlock-user/{userId}")
  @Produces("application/json")
  @Consumes()
  @ApiOperation(value = "unlock user", nickname = "unlockUser")
  public ResponseDTO<UserInfo> unlockUser(@NotNull @PathParam("userId") String userId,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountIdentifier, orgIdentifier, projectIdentifier),
        Resource.of(USER, userId), MANAGE_USER_PERMISSION);
    return ResponseDTO.newResponse(userInfoService.unlockUser(userId, accountIdentifier));
  }
}
