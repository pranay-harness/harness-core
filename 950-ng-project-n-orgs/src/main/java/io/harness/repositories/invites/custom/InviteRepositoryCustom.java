package io.harness.repositories.invites.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.Invite;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PL)
public interface InviteRepositoryCustom {
  Page<Invite> findAll(Criteria criteria, Pageable pageable);

  UpdateResult updateInvite(String inviteId, Update update);
}
