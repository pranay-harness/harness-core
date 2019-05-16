package software.wings.graphql.datafetcher.user;

import io.harness.beans.EmbeddedUser;
import software.wings.graphql.schema.type.QLUser;

public class UserController {
  public static QLUser populateUser(EmbeddedUser user) {
    if (user == null) {
      return null;
    }
    return QLUser.builder().id(user.getUuid()).name(user.getName()).email(user.getEmail()).build();
  }
}
