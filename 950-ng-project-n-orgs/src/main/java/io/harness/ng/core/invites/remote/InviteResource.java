package io.harness.ng.core.invites.remote;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.invites.remote.InviteMapper.toInviteList;
import static io.harness.utils.PageUtils.getNGPageResponse;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SortOrder;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.dto.CreateInviteListDTO;
import io.harness.ng.core.invites.dto.InviteDTO;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.Invite.InviteKeys;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.annotations.PublicApi;
import io.harness.utils.PageUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;

@Api("/invites")
@Path("/invites")
@Produces({"application/json", "text/yaml"})
@Consumes({"application/json", "text/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class InviteResource {
  private static final String INVALID_TOKEN_REDIRECT_URL = "account/%s/error?code=INVITE_EXPIRED";
  private final String ngUiBaseUrl;
  private final InviteService inviteService;

  @Inject
  InviteResource(InviteService inviteService, @Named("ngUiBaseUrl") String ngUiBaseUrl) {
    this.inviteService = inviteService;
    this.ngUiBaseUrl = ngUiBaseUrl;
  }

  @GET
  @ApiOperation(value = "Get all invites for the queried project/organization", nickname = "getInvites")
  @NextGenManagerAuth
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
    Page<InviteDTO> invites =
        inviteService.list(criteria, PageUtils.getPageRequest(pageRequest)).map(InviteMapper::writeDTO);
    return ResponseDTO.newResponse(getNGPageResponse(invites));
  }

  @ApiOperation(value = "Add a new invite for the specified project/organization", nickname = "sendInvite")
  @NextGenManagerAuth
  public ResponseDTO<List<InviteOperationResponse>> createInvitations(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("orgIdentifier") String orgIdentifier, @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @Valid CreateInviteListDTO createInviteListDTO) {
    projectIdentifier = stripToNull(projectIdentifier);
    orgIdentifier = stripToNull(orgIdentifier);
    List<InviteOperationResponse> inviteOperationResponses = new ArrayList<>();
    List<Invite> invites = toInviteList(createInviteListDTO, accountIdentifier, orgIdentifier, projectIdentifier);
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
  @Path("/verify")
  @PublicApi
  @ApiOperation(value = "Verify user invite", nickname = "verifyInvite")
  public Response verify(
      @QueryParam("token") @NotNull String jwtToken, @QueryParam("accountIdentifier") String accountIdentifier) {
    Optional<Invite> inviteOpt = inviteService.verify(jwtToken);
    URI redirectURI = inviteService.getRedirectURIForAcceptedInvite(inviteOpt.orElse(null));
    if (Objects.isNull(redirectURI)) {
      redirectURI = URI.create(ngUiBaseUrl + String.format(INVALID_TOKEN_REDIRECT_URL, accountIdentifier));
    }
    return Response.seeOther(redirectURI).build();
  }

  @PUT
  @Path("/{inviteId}")
  @ApiOperation(value = "Resend invite mail", nickname = "updateInvite")
  @NextGenManagerAuth
  public ResponseDTO<Optional<InviteDTO>> updateInvite(@PathParam("inviteId") @NotNull String inviteId,
      @NotNull @Valid InviteDTO inviteDTO, @QueryParam("accountIdentifier") String accountIdentifier) {
    NGAccess ngAccess = BaseNGAccess.builder().accountIdentifier(accountIdentifier).build();
    Invite invite = InviteMapper.toInvite(inviteDTO, ngAccess);
    invite.setId(inviteId);
    Optional<Invite> inviteOptional = inviteService.updateInvite(invite);
    return ResponseDTO.newResponse(inviteOptional.map(InviteMapper::writeDTO));
  }

  @DELETE
  @Path("/{inviteId}")
  @Consumes()
  @ApiOperation(value = "Delete a invite for the specified project/organization", nickname = "deleteInvite")
  @NextGenManagerAuth
  public ResponseDTO<Optional<InviteDTO>> delete(
      @PathParam("inviteId") @NotNull String inviteId, @QueryParam("accountIdentifier") String accountIdentifier) {
    Optional<Invite> inviteOptional = inviteService.deleteInvite(inviteId);
    return ResponseDTO.newResponse(inviteOptional.map(InviteMapper::writeDTO));
  }
}
