package software.wings.graphql.datafetcher.user;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import io.harness.beans.EmbeddedUser;
import lombok.experimental.UtilityClass;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.QLUser.QLUserBuilder;
import software.wings.graphql.schema.type.user.QLCreateUserInput;

import java.util.ArrayList;

@UtilityClass
public class UserController {
  public static QLUser populateUser(EmbeddedUser user) {
    if (user == null) {
      return null;
    }
    return QLUser.builder().id(user.getUuid()).name(user.getName()).email(user.getEmail()).build();
  }

  public static QLUser populateUser(User user, QLUserBuilder builder) {
    if (user == null) {
      return null;
    }
    return builder.id(user.getUuid())
        .name(user.getName())
        .email(user.getEmail())
        .isEmailVerified(user.isEmailVerified())
        .isTwoFactorAuthenticationEnabled(user.isTwoFactorAuthenticationEnabled())
        .isUserLocked(user.isUserLocked())
        .isPasswordExpired(user.isPasswordExpired())
        .isImportedFromIdentityProvider(user.isImported())
        .build();
  }

  public static User.Builder populateUser(
      User.Builder userBuilder, QLCreateUserInput qlCreateUserInput, final Account account) {
    return userBuilder.name(qlCreateUserInput.getName().trim())
        .email(qlCreateUserInput.getEmail().trim())
        .appId(GLOBAL_APP_ID)
        .accounts(new ArrayList<Account>() {
          { add(account); }
        });
  }
}
