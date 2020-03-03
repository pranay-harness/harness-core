package io.harness;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static io.harness.rule.OwnerRule.ABHINAV;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptionType;
import io.harness.testframework.graphql.QLTestObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.VaultConfig;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager.QLSecretManagerKeys;
import software.wings.graphql.schema.type.secretManagers.QLSecretManagerType;
import software.wings.service.intfc.security.SecretManagerConfigService;

import java.util.List;

public class SecretManagerTest extends GraphQLTest {
  @Inject private AccountGenerator accountGenerator;
  @Inject SecretManagerConfigService secretManagerConfigService;
  Account account;
  SecretManagerConfig secretManagerConfig;
  String SECRET_MANAGER_UUID = "1";
  String SECRET_MANAGER_NAME = "test secret manager";

  @Before
  public void setUp() {
    account = accountGenerator.ensureAccount(random(String.class), random(String.class), AccountType.TRIAL);
    secretManagerConfig = AwsSecretsManagerConfig.builder().name("test secret manager").build();
    secretManagerConfig.setUuid(SECRET_MANAGER_UUID);
    secretManagerConfig.setAccountId(account.getUuid());
    secretManagerConfigService.save(secretManagerConfig);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testSecretManagerQuery() {
    String secretManagerQueryPattern = MultilineStringMixin.$.GQL(/*
{
    secretManager(id: "%s"){
    secretManagerId
    name
  }
}
*/ SecretManagerTest.class);
    String query = String.format(secretManagerQueryPattern, SECRET_MANAGER_UUID);
    final QLTestObject qlSecretManagerObject = qlExecute(query, account.getUuid());
    assertThat(qlSecretManagerObject.get(QLSecretManagerKeys.secretManagerId)).isEqualTo(secretManagerConfig.getUuid());
    assertThat(qlSecretManagerObject.get(QLSecretManagerKeys.name)).isEqualTo(secretManagerConfig.getName());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testSecretManagerGetByNameQuery() {
    String secretManagerQueryPattern = MultilineStringMixin.$.GQL(/*
{
    secretManagerByName(name: "%s"){
    secretManagerId
    name
  }
}
*/ SecretManagerTest.class);
    String query = String.format(secretManagerQueryPattern, SECRET_MANAGER_NAME);
    final QLTestObject qlSecretManagerObject = qlExecute(query, account.getUuid());
    assertThat(qlSecretManagerObject.get(QLSecretManagerKeys.secretManagerId)).isEqualTo(secretManagerConfig.getUuid());
    assertThat(qlSecretManagerObject.get(QLSecretManagerKeys.name)).isEqualTo(secretManagerConfig.getName());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testSecretManagerListQuery() {
    String query = MultilineStringMixin.$.GQL(/*
{
  secretManagers(limit:10){
    nodes{
      secretManagerId
      name
    }
  }
}
*/ SecretManagerTest.class);
    addAzureAndAwsKmsSecretManagers();
    saveAwsSecretManagerWithDifferentAccount();
    final QLTestObject qlSecretManagerObject = qlExecute(query, account.getUuid());
    List secretManagerNodes = (List) qlSecretManagerObject.get("nodes");
    assertThat(secretManagerNodes.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({GraphQLTests.class, UnitTests.class})
  public void securityManagerTypesMustBeEqualInAPIandDB() {
    // LOCAL will differ in both the secret managers.
    assertThat(EncryptionType.values().length).isEqualTo(QLSecretManagerType.values().length + 1);
  }

  private void addAzureAndAwsKmsSecretManagers() {
    SecretManagerConfig awsKms = KmsConfig.builder().name("test aws kms").build();
    awsKms.setAccountId(account.getUuid());
    secretManagerConfigService.save(awsKms);

    SecretManagerConfig azureSecret = VaultConfig.builder().name("test secret vault").build();
    azureSecret.setAccountId(account.getUuid());
    secretManagerConfigService.save(azureSecret);
  }

  private void saveAwsSecretManagerWithDifferentAccount() {
    SecretManagerConfig awsKmsDifferentAccount =
        AwsSecretsManagerConfig.builder().name("test aws kms diff account").build();
    awsKmsDifferentAccount.setAccountId("random");
    secretManagerConfigService.save(awsKmsDifferentAccount);
  }
}
