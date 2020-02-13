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
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.graphql.schema.type.secrets.QLEncryptedText;

@Slf4j
public class CreateEncryptedTextTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  private String accountId;
  private String secretName = "secretName";

  @Before
  public void setup() {
    final Randomizer.Seed seed = new Randomizer.Seed(0);
    final OwnerManager.Owners owners = ownerManager.create();
    Account account = accountGenerator.ensurePredefined(seed, owners, AccountGenerator.Accounts.GENERIC_TEST);
    accountId = account.getUuid();
  }

  private String getCreateEncryptedTextInput() {
    String encryptedText = $GQL(/*{
         secretType: ENCRYPTED_TEXT,
         encryptedText: {
             name: "%s",
             value: "secret",
             secretManagerId: null
          }
 }*/ secretName);
    return encryptedText;
  }

  @Data
  public static class CreateEncryptedTextResult {
    String clientMutationId;
    QLEncryptedText secret;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testCreatingEncryptedText() {
    String query = $GQL(/*
    mutation{
       createSecret(input:%s){
           secret{
               ... on EncryptedText{
                 name
                 secretManagerId
                 id
               }
          }
       }
     }
   */ getCreateEncryptedTextInput());
    final QLTestObject qlTestObject = qlExecute(query, accountId);
    final CreateEncryptedTextResult result =
        JsonUtils.convertValue(qlTestObject.getMap(), CreateEncryptedTextResult.class);
    QLEncryptedText encryptedText = result.getSecret();
    assertThat(encryptedText.getId()).isNotNull();
    assertThat(encryptedText.getName()).isEqualTo(secretName);
  }
}