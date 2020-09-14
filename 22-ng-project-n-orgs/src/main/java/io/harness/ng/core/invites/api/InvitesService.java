package io.harness.ng.core.invites.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.entities.Invite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.Optional;

@OwnedBy(PL)
public interface InvitesService {
  InviteOperationResponse create(Invite invite);

  Invite resendInvitationMail(Invite invite);

  Optional<Invite> get(String inviteId);

  Page<Invite> list(Criteria criteria, Pageable pageable);

  Optional<Invite> deleteInvite(String inviteId);

  boolean sendInvitationMail(Invite invite);

  Optional<Invite> verify(String jwtToken);

  Optional<Invite> updateInvite(Invite invite);
}
