package io.harness.ng.core.invites.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.invites.remote.InviteAcceptResponse;
import io.harness.ng.accesscontrol.user.ACLAggregateFilter;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.invites.dto.CreateInviteDTO;
import io.harness.ng.core.invites.dto.InviteDTO;
import io.harness.ng.core.invites.dto.InviteOperationResponse;
import io.harness.ng.core.invites.entities.Invite;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface InviteService {
  InviteOperationResponse create(Invite invite);

  List<InviteOperationResponse> createInvitations(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, CreateInviteDTO createInviteDTO);

  Optional<Invite> getInvite(String inviteId, boolean allowDeleted);

  PageResponse<Invite> getInvites(Criteria criteria, PageRequest pageRequest);

  PageResponse<InviteDTO> getPendingInvites(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, PageRequest pageRequest, ACLAggregateFilter aclAggregateFilter);

  InviteAcceptResponse acceptInvite(String jwtToken);

  Optional<Invite> updateInvite(Invite invite);

  boolean completeInvite(Optional<Invite> inviteOpt);

  Optional<Invite> deleteInvite(String inviteId);

  Optional<Invite> getInviteFromToken(String jwtToken, boolean allowDeleted);

  boolean isUserPasswordSet(String accountIdentifier, String email);

  URI getRedirectUrl(InviteAcceptResponse inviteAcceptResponse, String email, String decodedEmail, String jwtToken);
}
