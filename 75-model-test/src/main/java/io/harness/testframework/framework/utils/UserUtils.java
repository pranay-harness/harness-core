package io.harness.testframework.framework.utils;

import io.harness.testframework.restutils.UserRestUtils;
import software.wings.beans.User;

import java.util.List;

public class UserUtils {
  public static User getUser(String bearerToken, String accountId, String emailId) {
    List<User> userList = UserRestUtils.getUserList(bearerToken, accountId);
    for (User user : userList) {
      if (user.getEmail().equals(emailId)) {
        return user;
      }
    }
    return null;
  }
}
