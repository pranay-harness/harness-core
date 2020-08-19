package io.harness.ng.core.user.services.api.impl;

import static io.harness.secretmanagerclient.utils.RestClientUtils.getResponse;

import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.ng.core.user.remote.UserClient;
import io.harness.ng.core.user.services.api.NgUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import software.wings.beans.User;

import java.util.List;

public class NgUserServiceImpl implements NgUserService {
  @Inject private UserClient userClient;

  @Override
  public Page<User> list(String accountIdentifier, String searchString, Pageable pageable) {
    //  @Ankush remove the offset and limit from the following statement because it is redundant pagination
    PageResponse<User> userPageResponse = getResponse(userClient.list(accountIdentifier, "0", "100", searchString));
    List<User> users = userPageResponse.getResponse();
    return new PageImpl<>(users, pageable, users.size());
  }

  public List<String> getUsernameFromEmail(String accountIdentifier, List<String> emailList) {
    return getResponse(userClient.getUsernameFromEmail(accountIdentifier, emailList));
  }
}
