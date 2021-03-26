package io.harness.ng.core.invites.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.InviteAcceptResponse;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.entities.Invite;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface InvitesService {
  InviteOperationResponse create(Invite invite);

  Invite resendInvitationMail(Invite invite);

  Optional<Invite> get(String inviteId);

  Page<Invite> list(Criteria criteria, Pageable pageable);

  Optional<Invite> deleteInvite(String inviteId);

  InviteAcceptResponse acceptInvite(String jwtToken);

  Optional<Invite> updateInvite(Invite invite);

  boolean completeInvite(String token);
}
