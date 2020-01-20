package io.harness;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.VARDAN_BANSAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.User;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;
import software.wings.graphql.schema.type.User.QLUserConnection;

public class UserTest extends GraphQLTest {
  @Inject private AccountGenerator accountGenerator;

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testUserQuery() {
    String userQueryPattern = MultilineStringMixin.$.GQL(/*
    {
        user(id: "%s"){
        id
        name
        email
        isEmailVerified
        isTwoFactorAuthenticationEnabled
        isUserLocked
        isPasswordExpired
        isImportedFromIdentityProvider
      }
    }
*/ UserTest.class);
    String query = String.format(userQueryPattern, "userId", 1);
    final Account account =
        accountGenerator.ensureAccount(random(String.class), random(String.class), AccountType.TRIAL);
    final User user = accountGenerator.ensureUser(
        "userId", random(String.class), random(String.class), random(String.class).toCharArray(), account);

    final QLTestObject qlUserObject = qlExecute(query, account.getUuid());
    assertThat(qlUserObject.get(QLUserKeys.id)).isEqualTo(user.getUuid());
    assertThat(qlUserObject.get(QLUserKeys.name)).isEqualTo(user.getName());
    assertThat(qlUserObject.get(QLUserKeys.email)).isEqualTo(user.getEmail());
    assertThat(qlUserObject.get(QLUserKeys.isEmailVerified)).isEqualTo(user.isEmailVerified());
    assertThat(qlUserObject.get(QLUserKeys.isTwoFactorAuthenticationEnabled))
        .isEqualTo(user.isTwoFactorAuthenticationEnabled());
    assertThat(qlUserObject.get(QLUserKeys.isUserLocked)).isEqualTo(user.isUserLocked());
    assertThat(qlUserObject.get(QLUserKeys.isPasswordExpired)).isEqualTo(user.isPasswordExpired());
    assertThat(qlUserObject.get(QLUserKeys.isImportedFromIdentityProvider)).isEqualTo(user.isImported());
  }

  @Test
  @Owner(developers = VARDAN_BANSAL)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testUserQueryPaginated() {
    String userQueryPattern = MultilineStringMixin.$.GQL(/*
        {
      users(limit:%d){
        nodes{
          id
          name
          email
          isEmailVerified
          isTwoFactorAuthenticationEnabled
          isUserLocked
          isPasswordExpired
          isImportedFromIdentityProvider
        }
      }
    }
*/ UserTest.class);
    String query = String.format(userQueryPattern, 1);
    final Account account =
        accountGenerator.ensureAccount(random(String.class), random(String.class), AccountType.TRIAL);
    User user = accountGenerator.ensureUser(
        "userId", random(String.class), random(String.class), random(String.class).toCharArray(), account);
    user.setEmailVerified(true);
    user.setTwoFactorAuthenticationEnabled(false);
    user.setUserLocked(false);
    user.setPasswordExpired(false);
    user.setImported(false);

    QLUserConnection userConnection = qlExecute(QLUserConnection.class, query, account.getUuid());
    assertThat(userConnection.getNodes().size()).isEqualTo(1);
    QLUser qlUser = userConnection.getNodes().get(0);

    assertThat(qlUser.getId()).isEqualTo(user.getUuid());
    assertThat(qlUser.getName()).isEqualTo(user.getName());
    assertThat(qlUser.getEmail()).isEqualTo(user.getEmail());
    assertThat(qlUser.getIsEmailVerified()).isEqualTo(user.isEmailVerified());
    assertThat(qlUser.getIsTwoFactorAuthenticationEnabled()).isEqualTo(user.isTwoFactorAuthenticationEnabled());
    assertThat(qlUser.getIsUserLocked()).isEqualTo(user.isUserLocked());
    assertThat(qlUser.getIsPasswordExpired()).isEqualTo(user.isPasswordExpired());
    assertThat(qlUser.getIsImportedFromIdentityProvider()).isEqualTo(user.isImported());
  }
}
