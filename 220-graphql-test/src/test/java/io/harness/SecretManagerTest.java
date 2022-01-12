/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.VOJIN;

import static com.google.common.collect.Sets.newHashSet;
import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.SecretManagerConfig;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.AccountGenerator;
import io.harness.multiline.MultilineStringMixin;
import io.harness.rule.Owner;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.security.encryption.EncryptionType;
import io.harness.serializer.JsonUtils;
import io.harness.testframework.graphql.QLTestObject;

import software.wings.beans.Account;
import software.wings.beans.AccountType;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.graphql.schema.type.QLEnvFilterType;
import software.wings.graphql.schema.type.QLGenericFilterType;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager.QLSecretManagerKeys;
import software.wings.graphql.schema.type.secretManagers.QLSecretManagerType;
import software.wings.graphql.schema.type.secrets.QLAppEnvScope;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.UsageRestrictions;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SecretManagerTest extends GraphQLTest {
  @Inject private AccountGenerator accountGenerator;
  @Inject SecretManagerConfigService secretManagerConfigService;
  Account account;
  SecretManagerConfig secretManagerConfig;
  String SECRET_MANAGER_NAME = "test secret manager";

  @Before
  public void setUp() {
    account = accountGenerator.ensureAccount(random(String.class), random(String.class), AccountType.TRIAL);
    secretManagerConfig = AwsSecretsManagerConfig.builder().name(SECRET_MANAGER_NAME).build();
    secretManagerConfig.setAccountId(account.getUuid());
    secretManagerConfig.setUsageRestrictions(createUsageRestrictions());
    secretManagerConfigService.save(secretManagerConfig);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testSecretManagerQuery() {
    String secretManagerQueryPattern = MultilineStringMixin.$.GQL(/*
{
    secretManager(secretManagerId: "%s"){
    id
    name
  }
}
*/ SecretManagerTest.class);
    String query = String.format(secretManagerQueryPattern, secretManagerConfig.getUuid());
    final QLTestObject qlSecretManagerObject = qlExecute(query, account.getUuid());
    assertThat(qlSecretManagerObject.get(QLSecretManagerKeys.id)).isEqualTo(secretManagerConfig.getUuid());
    assertThat(qlSecretManagerObject.get(QLSecretManagerKeys.name)).isEqualTo(secretManagerConfig.getName());
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testSecretManagerGetByNameQuery() {
    String secretManagerQueryPattern = MultilineStringMixin.$.GQL(/*
{
    secretManagerByName(name: "%s"){
    id
    name
  }
}
*/ SecretManagerTest.class);
    String query = String.format(secretManagerQueryPattern, SECRET_MANAGER_NAME);
    final QLTestObject qlSecretManagerObject = qlExecute(query, account.getUuid());
    assertThat(qlSecretManagerObject.get(QLSecretManagerKeys.id)).isEqualTo(secretManagerConfig.getUuid());
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
      id
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

  @Test
  @Owner(developers = VOJIN)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testSecretManagerQuery_usageScope() {
    String secretManagerQueryPattern = MultilineStringMixin.$.GQL(/*
{
    secretManager(secretManagerId: "%s"){
    usageScope{
      appEnvScopes{
        application {
          appId
          filterType
        }
        environment {
          envId
          filterType
        }
      }
    }
  }
}
*/ SecretManagerTest.class);
    String query = String.format(secretManagerQueryPattern, secretManagerConfig.getUuid());
    final QLTestObject qlSecretManagerObject = qlExecute(query, account.getUuid());

    QLUsageScope usageScope =
        JsonUtils.convertValue(qlSecretManagerObject.get(QLSecretManagerKeys.usageScope), QLUsageScope.class);
    assertThat(usageScope).isNotNull();

    Set<QLAppEnvScope> appEnvScopes = usageScope.getAppEnvScopes();
    assertThat(appEnvScopes.size()).isEqualTo(1);

    QLAppEnvScope appEnvScope = Iterables.getOnlyElement(appEnvScopes);
    assertThat(appEnvScope.getApplication().getFilterType()).isEqualTo(QLGenericFilterType.ALL);
    assertThat(appEnvScope.getEnvironment().getFilterType()).isEqualTo(QLEnvFilterType.PRODUCTION_ENVIRONMENTS);
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

  private UsageRestrictions createUsageRestrictions() {
    GenericEntityFilter appFilter =
        GenericEntityFilter.builder().filterType(GenericEntityFilter.FilterType.ALL).build();
    EnvFilter envFilter = EnvFilter.builder().filterTypes(newHashSet(EnvFilter.FilterType.PROD)).build();
    UsageRestrictions.AppEnvRestriction appEnvRestriction =
        UsageRestrictions.AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    return UsageRestrictions.builder().appEnvRestrictions(newHashSet(appEnvRestriction)).build();
  }
}
