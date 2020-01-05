package software.wings.graphql.datafetcher.user;

import io.harness.beans.EmbeddedUser;
import lombok.experimental.UtilityClass;
import software.wings.graphql.schema.type.QLUser;

@UtilityClass
public class UserController {
  public static QLUser populateUser(EmbeddedUser user) {
    if (user == null) {
      return null;
    }
    return QLUser.builder().id(user.getUuid()).name(user.getName()).email(user.getEmail()).build();
  }
}
