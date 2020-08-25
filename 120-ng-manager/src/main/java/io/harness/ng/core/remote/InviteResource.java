package io.harness.ng.core.remote;

import static io.harness.ng.core.remote.InviteMapper.toInviteList;
import static io.harness.utils.PageUtils.getNGPageResponse;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.api.InvitesService;
import io.harness.ng.core.dto.CreateInviteListDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.InviteDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.models.Invite;
import io.harness.ng.core.models.Invite.InviteKeys;
import io.harness.ng.core.user.services.api.NgUserService;
import io.harness.utils.PageUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.query.Criteria;
import software.wings.service.impl.InviteOperationResponse;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/invites")
@Path("/organizations/{orgIdentifier}/projects/{projectIdentifier}/invites")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
public class InviteResource {
  private final InvitesService invitesService;
  private final NgUserService ngUserService;

  @GET
  @ApiOperation(value = "Get all invites for the queried project", nickname = "getInvites")
  public ResponseDTO<NGPageResponse<InviteDTO>> get(@QueryParam("accountIdentifier") @NotEmpty String accountIdentifier,
      @PathParam("orgIdentifier") @NotEmpty String orgIdentifier,
      @PathParam("projectIdentifier") @NotEmpty String projectIdentifier,
      @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("50") @Max(100) int size,
      @QueryParam("sort") List<String> sort) {
    Criteria criteria = Criteria.where(InviteKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(InviteKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(InviteKeys.projectIdentifier)
                            .is(projectIdentifier);
    Page<InviteDTO> invites =
        invitesService.list(criteria, PageUtils.getPageRequest(page, size, sort)).map(InviteMapper::writeDTO);
    return ResponseDTO.newResponse(getNGPageResponse(invites));
  }

  @POST
  @ApiOperation(value = "Add a new invite for the specified project", nickname = "sendInvite")
  public ResponseDTO<List<InviteOperationResponse>> createInvitations(
      @PathParam("projectIdentifier") @NotNull String projectIdentifier,
      @PathParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @NotNull @Valid CreateInviteListDTO createInviteListDTO) {
    NGAccess ngAccess = BaseNGAccess.builder()
                            .accountIdentifier(accountIdentifier)
                            .orgIdentifier(orgIdentifier)
                            .projectIdentifier(projectIdentifier)
                            .build();

    List<InviteOperationResponse> inviteOperationResponses = new ArrayList<>();
    List<String> usernames = ngUserService.getUsernameFromEmail(accountIdentifier, createInviteListDTO.getUsers());
    List<String> userMails = createInviteListDTO.getUsers();
    for (int i = 0; i < usernames.size(); i++) {
      if (usernames.get(i) == null) {
        String defaultName = userMails.get(i).split("@", 2)[0];
        usernames.set(i, defaultName);
      }
    }
    List<Invite> invites = toInviteList(createInviteListDTO, usernames, ngAccess);

    for (Invite invite : invites) {
      try {
        InviteOperationResponse response = invitesService.create(invite);
        inviteOperationResponses.add(response);
      } catch (DuplicateFieldException ex) {
        logger.error("error: ", ex);
      }
    }
    return ResponseDTO.newResponse(inviteOperationResponses);
  }
}
