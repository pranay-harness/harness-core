package io.harness.repositories.user.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.user.entities.UserMembership;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PL)
public interface UserMembershipRepositoryCustom {
  List<UserMembership> findAll(Criteria criteria);

  UserMembership findOne(Criteria criteria);

  Page<UserMembership> findAll(Criteria criteria, Pageable pageable);

  Page<String> findAllUserIds(Criteria criteria, Pageable pageable);

  UserMembership update(String userId, Update update);

  long insertAllIgnoringDuplicates(List<UserMembership> userMemberships);
}
