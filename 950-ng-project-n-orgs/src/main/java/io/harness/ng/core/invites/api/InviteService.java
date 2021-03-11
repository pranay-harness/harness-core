package io.harness.ng.core.invites.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.invites.InviteOperationResponse;
import io.harness.ng.core.invites.entities.Invite;

import java.net.URI;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface InviteService {
  InviteOperationResponse create(Invite invite);

  Optional<Invite> get(String inviteId);

  PageResponse<Invite> getAllPendingInvites(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, PageRequest pageRequest);

  Page<Invite> list(Criteria criteria, Pageable pageable);

  Optional<Invite> deleteInvite(String inviteId);

  Optional<Invite> verify(String jwtToken);

  Optional<Invite> updateInvite(Invite invite);

  URI getRedirectURIForAcceptedInvite(Invite invite);

  boolean newUserEventHandler(String userId, String emailId);
}
