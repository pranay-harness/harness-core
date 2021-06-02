package io.harness.ng.core.invites.remote;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_USER_PERMISSION;
import static io.harness.ng.accesscontrol.PlatformResourceTypes.USER;
import static io.harness.ng.core.invites.remote.InviteMapper.toInviteList;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.exception.DuplicateFieldException;
import io.harness.invites.remote.InviteAcceptResponse;
import io.harness.ng.accesscontrol.user.ACLAggregateFilter;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.dto.CreateInviteDTO;
import io.harness.ng.core.invites.dto.InviteDTO;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.annotations.PublicApi;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Api("invites")
@Path("invites")
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class InviteResource {
  private final InviteService inviteService;

  @Inject
  InviteResource(InviteService inviteService) {
    this.inviteService = inviteService;
  }

  @GET
  @ApiOperation(value = "Get all invites for the queried project/organization", nickname = "getInvites")
  public ResponseDTO<PageResponse<InviteDTO>> getInvites(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @BeanParam PageRequest pageRequest) {
    projectIdentifier = stripToNull(projectIdentifier);
    if (isEmpty(pageRequest.getSortOrders())) {
      SortOrder order =
          SortOrder.Builder.aSortOrder().withField(InviteKeys.createdAt, SortOrder.OrderType.DESC).build();
      pageRequest.setSortOrders(ImmutableList.of(order));
    }
    Criteria criteria = Criteria.where(InviteKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(InviteKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(InviteKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(InviteKeys.approved)
                            .is(Boolean.FALSE)
                            .and(InviteKeys.deleted)
                            .is(Boolean.FALSE);
    PageResponse<InviteDTO> invites = inviteService.getInvites(criteria, pageRequest).map(InviteMapper::writeDTO);
    return ResponseDTO.newResponse(invites);
  }

  @POST
  @Path("aggregate")
  @ApiOperation(value = "Get a page of pending users for access control", nickname = "getPendingUsersAggregated")
  @NGAccessControlCheck(resourceType = USER, permission = VIEW_USER_PERMISSION)
  public ResponseDTO<PageResponse<InviteDTO>> getPendingInvites(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @QueryParam("searchTerm") String searchTerm, @BeanParam PageRequest pageRequest,
      ACLAggregateFilter aclAggregateFilter) {
    PageResponse<InviteDTO> inviteDTOs = inviteService.getPendingInvites(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, pageRequest, aclAggregateFilter);
    return ResponseDTO.newResponse(inviteDTOs);
  }

  @POST
  @ApiOperation(value = "Add a new invite for the specified project/organization", nickname = "sendInvite")
  public ResponseDTO<List<InviteOperationResponse>> createInvitations(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Valid CreateInviteDTO createInviteDTO) {
    projectIdentifier = stripToNull(projectIdentifier);
    orgIdentifier = stripToNull(orgIdentifier);
    List<InviteOperationResponse> inviteOperationResponses = new ArrayList<>();
    List<Invite> invites = toInviteList(createInviteDTO, accountIdentifier, orgIdentifier, projectIdentifier);
    for (Invite invite : invites) {
      try {
        InviteOperationResponse response = inviteService.create(invite);
        inviteOperationResponses.add(response);
      } catch (DuplicateFieldException ex) {
        log.error("error: ", ex);
      }
    }
    return ResponseDTO.newResponse(inviteOperationResponses);
  }

  @GET
  @Path("accept")
  @ApiOperation(value = "Verify user invite", nickname = "verifyInvite", hidden = true)
  public ResponseDTO<InviteAcceptResponse> accept(@QueryParam("token") @NotNull String jwtToken) {
    return ResponseDTO.newResponse(inviteService.acceptInvite(jwtToken));
  }

  @GET
  @Path("verify")
  @ApiOperation(
      value = "Verify user invite with the new NG Auth UI flow", nickname = "verifyInviteViaNGAuthUi", hidden = true)
  @PublicApi
  public Response
  verifyInviteViaNGAuthUi(@QueryParam("token") @NotNull String jwtToken,
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier, @QueryParam("email") @NotNull String email) {
    InviteAcceptResponse inviteAcceptResponse = inviteService.acceptInvite(jwtToken);
    URI redirectURL = inviteService.getRedirectUrl(inviteAcceptResponse, email, jwtToken);
    return Response.seeOther(redirectURL).build();
  }

  @GET
  @Path("complete")
  @ApiOperation(value = "Complete user invite", nickname = "completeInvite", hidden = true)
  public ResponseDTO<Boolean> completeInvite(@QueryParam("token") String token) {
    return ResponseDTO.newResponse(inviteService.completeInvite(token));
  }

  @PUT
  @Path("{inviteId}")
  @ApiOperation(value = "Resend invite mail", nickname = "updateInvite")
  public ResponseDTO<Optional<InviteDTO>> updateInvite(@PathParam("inviteId") @NotNull String inviteId,
      @NotNull @Valid InviteDTO inviteDTO, @QueryParam("accountIdentifier") String accountIdentifier) {
    NGAccess ngAccess = BaseNGAccess.builder().accountIdentifier(accountIdentifier).build();
    Invite invite = InviteMapper.toInvite(inviteDTO, ngAccess);
    invite.setId(inviteId);
    Optional<Invite> inviteOptional = inviteService.updateInvite(invite);
    return ResponseDTO.newResponse(inviteOptional.map(InviteMapper::writeDTO));
  }

  @DELETE
  @Path("{inviteId}")
  @ApiOperation(value = "Delete a invite for the specified project/organization", nickname = "deleteInvite")
  @Produces("application/json")
  @Consumes()
  public ResponseDTO<Optional<InviteDTO>> delete(@PathParam("inviteId") @NotNull String inviteId) {
    Optional<Invite> inviteOptional = inviteService.deleteInvite(inviteId);
    return ResponseDTO.newResponse(inviteOptional.map(InviteMapper::writeDTO));
  }
}
