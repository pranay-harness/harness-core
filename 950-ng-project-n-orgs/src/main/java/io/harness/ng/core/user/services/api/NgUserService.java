package io.harness.ng.core.user.services.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.user.User;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface NgUserService {
  Page<User> list(String accountIdentifier, String searchString, Pageable page);

  List<UserProjectMap> listUserProjectMap(Criteria criteria);

  Optional<User> getUserFromEmail(String accountId, String email);

  List<String> getUsernameFromEmail(String accountIdentifier, List<String> emailList);

  Optional<UserProjectMap> getUserProjectMap(
      String uuid, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  void createUserProjectMap(Invite invite, User user);

  List<User> getUsersByIds(List<String> userIds);

  UserProjectMap createUserProjectMap(UserProjectMap userProjectMap);
}
