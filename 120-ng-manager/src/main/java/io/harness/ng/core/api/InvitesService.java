package io.harness.ng.core.api;

import io.harness.ng.core.models.Invite;
import io.harness.ng.core.models.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import software.wings.service.impl.InviteOperationResponse;

import java.util.Optional;

public interface InvitesService {
  InviteOperationResponse create(Invite invite);

  Optional<Invite> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String email, Role role);

  Page<Invite> list(Criteria criteria, Pageable pageable);

  Optional<Invite> delete(String inviteId);
}
