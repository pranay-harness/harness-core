package io.harness.secret;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.GraphQLTest;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import io.harness.testframework.graphql.QLTestObject;
import lombok.Data;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;

public class UpdateWinRMCredentialsTest extends GraphQLTest {
  @Inject private AccountGenerator accountGenerator;
  @Inject private OwnerManager ownerManager;
  @Inject private WinRMCredentialHelper winRMCredentialHelper;

  private String updatedUserName = "updatedUserName";
  private String updatedName = "updatedName";
  private String updatedPassword = "updatedPassword";
  private int updatedPort = 222;
  private String accountId;
  private String secretId;

  @Before
  public void setup() {
    final OwnerManager.Owners owners = ownerManager.create();
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
    secretId = winRMCredentialHelper.CreateWinRMCredential();
  }
  private String getUpdateWinRMCredentialNameInput() {
    String input = $GQL(/*
    {
    secretType: WINRM_CREDENTIAL,
    id: "%s",
    winRMCredential: {
        name: "%s",
        userName: "%s",
        authenticationScheme:  NTLM,
        password: "%s"
        useSSL: false,
        skipCertCheck: false,
        port: %d
     }
    }
    */ secretId, updatedName, updatedUserName, updatedPassword, updatedPort);
    return input;
  }

  @Data
  private static class WinRMSecret {
    String name;
    String id;
    int port;
    boolean skipCertCheck;
    boolean useSSL;
    String userName;
  }

  @Data
  private static class UpdateWinRMResult {
    String clientMutationId;
    WinRMSecret secret;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testUpdatingWinRMCredentialName() {
    String query = $GQL(/*
    mutation{
        updateSecret(input:%s){
                  clientMutationId
            secret{
            ... on WinRMCredential{
            name
            id
            port
            skipCertCheck
            useSSL
            userName
            authenticationScheme
            }
        }
     }
    }
    */ getUpdateWinRMCredentialNameInput());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    final UpdateWinRMResult result = JsonUtils.convertValue(qlTestObject.getMap(), UpdateWinRMResult.class);
    assertThat(result.getSecret()).isNotNull();
    assertThat(result.getSecret().getId()).isNotNull();
    assertThat(result.getSecret().getName()).isEqualTo(updatedName);
    assertThat(result.getSecret().getPort()).isEqualTo(updatedPort);
    assertThat(result.getSecret().getUserName()).isEqualTo(updatedUserName);
    assertThat(result.getSecret().isUseSSL()).isEqualTo(false);
    assertThat(result.getSecret().isSkipCertCheck()).isEqualTo(false);
  }
}
