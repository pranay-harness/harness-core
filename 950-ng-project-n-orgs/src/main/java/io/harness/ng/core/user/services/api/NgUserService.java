package io.harness.ng.core.user.services.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.invites.entities.Invite;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.user.UserInfo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
public interface NgUserService {
  Page<UserInfo> list(String accountIdentifier, String searchString, Pageable page);

  List<UserProjectMap> listUserProjectMap(Criteria criteria);

  Optional<UserInfo> getUserFromEmail(String email);

  List<String> getUsernameFromEmail(String accountIdentifier, List<String> emailList);

  Optional<UserProjectMap> getUserProjectMap(
      String uuid, String accountIdentifier, String orgIdentifier, String projectIdentifier);

  boolean createUserProjectMap(Invite invite, UserInfo user);

  List<UserInfo> getUsersByIds(List<String> userIds);

  UserProjectMap createUserProjectMap(UserProjectMap userProjectMap);

  boolean isUserInAccount(String accountId, String userId);

  void removeUserFromScope(String userId, String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
