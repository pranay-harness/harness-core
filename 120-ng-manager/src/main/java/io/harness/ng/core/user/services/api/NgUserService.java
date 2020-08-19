package io.harness.ng.core.user.services.api;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import software.wings.beans.User;

import java.util.List;

public interface NgUserService {
  Page<User> list(String accountIdentifier, String searchString, Pageable page);

  List<String> getUsernameFromEmail(String accountIdentifier, List<String> emailList);
}
