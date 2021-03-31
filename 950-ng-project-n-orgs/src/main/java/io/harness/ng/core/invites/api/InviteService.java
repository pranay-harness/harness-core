package io.harness.ng.core.invites.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.invites.InviteAcceptResponse;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.entities.Invite;

import java.util.Optional;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface InviteService {
  InviteOperationResponse create(Invite invite);

  Optional<Invite> getInvite(String inviteId);

  PageResponse<Invite> getInvites(Criteria criteria, PageRequest pageRequest);

  InviteAcceptResponse acceptInvite(String jwtToken);

  Optional<Invite> updateInvite(Invite invite);

  boolean completeInvite(String token);

  Optional<Invite> deleteInvite(String inviteId);

  boolean deleteInvite(String accountIdentifier, String orgIdentifier, String projectIdentifier, String emailId);
}
