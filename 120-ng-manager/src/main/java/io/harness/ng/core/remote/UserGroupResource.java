package io.harness.ng.core.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;
import static io.harness.ng.core.utils.UserGroupMapper.toDTO;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.dto.UserGroupFilterDTO;
import io.harness.ng.core.entities.UserGroup;
import io.harness.ng.core.utils.UserGroupMapper;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.Valid;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import retrofit2.http.Body;

@OwnedBy(PL)
@Api("user-groups")
@Path("user-groups")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class UserGroupResource {
  private final UserGroupService userGroupService;

  @POST
  @ApiOperation(value = "Create a User Group", nickname = "postUserGroup")
  public ResponseDTO<UserGroupDTO> create(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @Valid UserGroupDTO userGroupDTO) {
    validateScopes(accountIdentifier, orgIdentifier, projectIdentifier, userGroupDTO);
    userGroupDTO.setAccountIdentifier(accountIdentifier);
    userGroupDTO.setOrgIdentifier(orgIdentifier);
    userGroupDTO.setProjectIdentifier(projectIdentifier);
    UserGroup userGroup = userGroupService.create(userGroupDTO);
    return ResponseDTO.newResponse(Long.toString(userGroup.getVersion()), toDTO(userGroup));
  }

  @PUT
  @ApiOperation(value = "Update a User Group", nickname = "putUserGroup")
  public ResponseDTO<UserGroupDTO> update(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotNull @Valid UserGroupDTO userGroupDTO) {
    validateScopes(accountIdentifier, orgIdentifier, projectIdentifier, userGroupDTO);
    userGroupDTO.setAccountIdentifier(accountIdentifier);
    userGroupDTO.setOrgIdentifier(orgIdentifier);
    userGroupDTO.setProjectIdentifier(projectIdentifier);
    UserGroup userGroup = userGroupService.update(userGroupDTO);
    return ResponseDTO.newResponse(Long.toString(userGroup.getVersion()), toDTO(userGroup));
  }

  @GET
  @Path("{identifier}")
  @ApiOperation(value = "Get a User Group", nickname = "getUserGroup")
  public ResponseDTO<UserGroupDTO> get(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotEmpty @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier) {
    Optional<UserGroup> userGroupOptional =
        userGroupService.get(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return userGroupOptional
        .map(userGroup -> ResponseDTO.newResponse(Long.toString(userGroup.getVersion()), toDTO(userGroup)))
        .orElseGet(() -> ResponseDTO.newResponse(null));
  }

  @DELETE
  @Path("{identifier}")
  @ApiOperation(value = "Delete a User Group", nickname = "deleteUserGroup")
  public ResponseDTO<UserGroupDTO> delete(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotEmpty @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier) {
    UserGroup userGroup = userGroupService.delete(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return ResponseDTO.newResponse(Long.toString(userGroup.getVersion()), toDTO(userGroup));
  }

  @GET
  @ApiOperation(value = "Get User Group List", nickname = "getUserGroupList")
  public ResponseDTO<PageResponse<UserGroupDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    if (hasNone(pageRequest.getSortOrders())) {
      SortOrder order = SortOrder.Builder.aSortOrder().withField("lastModifiedAt", SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    Page<UserGroupDTO> page =
        userGroupService
            .list(accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, getPageRequest(pageRequest))
            .map(UserGroupMapper::toDTO);
    return ResponseDTO.newResponse(getNGPageResponse(page));
  }

  @POST
  @Path("batch")
  @ApiOperation(value = "Get Batch User Group List", nickname = "getBatchUserGroupList")
  public ResponseDTO<List<UserGroupDTO>> list(@Body @NotNull UserGroupFilterDTO userGroupFilterDTO) {
    List<UserGroupDTO> userGroups =
        userGroupService.list(userGroupFilterDTO).stream().map(UserGroupMapper::toDTO).collect(Collectors.toList());
    return ResponseDTO.newResponse(userGroups);
  }

  @GET
  @Path("{identifier}/member/{userIdentifier}")
  @ApiOperation(value = "Check if the user is part of the user group", nickname = "checkMember")
  public ResponseDTO<Boolean> checkMember(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @PathParam("userIdentifier") String userIdentifier) {
    boolean isMember =
        userGroupService.checkMember(accountIdentifier, orgIdentifier, projectIdentifier, identifier, userIdentifier);
    return ResponseDTO.newResponse(isMember);
  }

  @PUT
  @Path("{identifier}/member/{userIdentifier}")
  @ApiOperation(value = "Add a user to the user group", nickname = "addMember")
  public ResponseDTO<UserGroupDTO> addMember(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @PathParam("userIdentifier") String userIdentifier) {
    UserGroup userGroup =
        userGroupService.addMember(accountIdentifier, orgIdentifier, projectIdentifier, identifier, userIdentifier);
    return ResponseDTO.newResponse(Long.toString(userGroup.getVersion()), toDTO(userGroup));
  }

  @DELETE
  @Path("{identifier}/member/{userIdentifier}")
  @ApiOperation(value = "Remove a user from the user group", nickname = "removeMember")
  public ResponseDTO<UserGroupDTO> removeMember(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @PathParam(NGCommonEntityConstants.IDENTIFIER_KEY) String identifier,
      @PathParam("userIdentifier") String userIdentifier) {
    UserGroup userGroup =
        userGroupService.removeMember(accountIdentifier, orgIdentifier, projectIdentifier, identifier, userIdentifier);
    return ResponseDTO.newResponse(Long.toString(userGroup.getVersion()), toDTO(userGroup));
  }

  public static void validateScopes(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, UserGroupDTO userGroupDTO) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(accountIdentifier, userGroupDTO.getAccountIdentifier()),
                               Pair.of(orgIdentifier, userGroupDTO.getOrgIdentifier()),
                               Pair.of(projectIdentifier, userGroupDTO.getProjectIdentifier())),
        true);
  }
}
