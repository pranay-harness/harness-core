package io.harness.ng.core.remote;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.utils.UserGroupMapper.toDTO;
import static io.harness.utils.PageUtils.getNGPageResponse;
import static io.harness.utils.PageUtils.getPageRequest;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.entities.UserGroup;
import io.harness.ng.core.utils.UserGroupMapper;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;

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
    UserGroup userGroup = userGroupService.create(accountIdentifier, orgIdentifier, projectIdentifier, userGroupDTO);
    return ResponseDTO.newResponse(Long.toString(userGroup.getVersion()), toDTO(userGroup));
  }

  @GET
  @ApiOperation(value = "Get User Group List", nickname = "getUserGroupList")
  public ResponseDTO<PageResponse<UserGroupDTO>> list(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm, @BeanParam PageRequest pageRequest) {
    if (isEmpty(pageRequest.getSortOrders())) {
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
  public ResponseDTO<List<UserGroupDTO>> list(@NotNull List<String> userGroupsIds) {
    List<UserGroupDTO> userGroups =
        userGroupService.list(userGroupsIds).stream().map(UserGroupMapper::toDTO).collect(Collectors.toList());
    return ResponseDTO.newResponse(userGroups);
  }
}
