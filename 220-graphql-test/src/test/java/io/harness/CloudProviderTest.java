/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.SRINIVAS;

import static software.wings.graphql.schema.type.cloudProvider.QLAwsCloudProvider.QLCloudProviderKeys;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.graphql.QLTestObject;

import software.wings.beans.SettingAttribute;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderConnection.QLCloudProviderConnectionKeys;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class CloudProviderTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject private SettingGenerator settingGenerator;

  private static String cloudProviderQueryPattern = $.GQL(/*
{
  cloudProvider(cloudProviderId: "%s") {
    id
    name
    createdAt
    createdBy {
      id
    }
  }
}*/ CloudProviderTest.class);

  @Test
  @Owner(developers = RUSHABH)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryCloudProvider() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());
    {
      final SettingAttribute settingAttribute = settingGenerator.ensureAwsTest(seed, owners);
      assertThat(settingAttribute).isNotNull();

      String query = String.format(cloudProviderQueryPattern, settingAttribute.getUuid());

      QLTestObject qlAwsCP = qlExecute(query, settingAttribute.getAccountId());
      assertThat(qlAwsCP.get(QLCloudProviderKeys.id)).isEqualTo(settingAttribute.getUuid());
      assertThat(qlAwsCP.get(QLCloudProviderKeys.name)).isEqualTo(settingAttribute.getName());
      assertThat(qlAwsCP.get(QLCloudProviderKeys.createdAt)).isEqualTo(settingAttribute.getCreatedAt());
      assertThat(qlAwsCP.sub(QLCloudProviderKeys.createdBy).get(QLUserKeys.id))
          .isEqualTo(settingAttribute.getCreatedBy().getUuid());
    }

    //    {
    //      final SettingAttribute settingAttribute = settingGenerator.ensurePhysicalDataCenter(seed, owners);
    //      assertThat(settingAttribute).isNotNull();
    //
    //      String query = String.format(cloudProviderQueryPattern, settingAttribute.getUuid());
    //
    //      QLTestObject qlAwsCP = qlExecute(query, settingAttribute.getAccountId());
    //      assertThat(qlAwsCP.get(QLCloudProviderKeys.id)).isEqualTo(settingAttribute.getUuid());
    //      assertThat(qlAwsCP.get(QLCloudProviderKeys.name)).isEqualTo(settingAttribute.getName());
    //      assertThat(qlAwsCP.get(QLCloudProviderKeys.createdAt))
    //          .isEqualTo(GraphQLDateTimeScalar.convertToString(settingAttribute.getCreatedAt()));
    //      assertThat(qlAwsCP.sub(QLCloudProviderKeys.createdBy).get(QLUserKeys.id))
    //          .isEqualTo(settingAttribute.getCreatedBy().getUuid());
    //    }

    //    {
    //      final SettingAttribute settingAttribute = settingGenerator.ensureAzureTestCloudProvider(seed, owners);
    //      assertThat(settingAttribute).isNotNull();
    //
    //      String query = String.format(cloudProviderQueryPattern, settingAttribute.getUuid());
    //
    //      QLTestObject qlAwsCP = qlExecute(query, settingAttribute.getAccountId());
    //      assertThat(qlAwsCP.get(QLCloudProviderKeys.id)).isEqualTo(settingAttribute.getUuid());
    //      assertThat(qlAwsCP.get(QLCloudProviderKeys.name)).isEqualTo(settingAttribute.getName());
    //      assertThat(qlAwsCP.get(QLCloudProviderKeys.createdAt))
    //          .isEqualTo(GraphQLDateTimeScalar.convertToString(settingAttribute.getCreatedAt()));
    //      assertThat(qlAwsCP.sub(QLCloudProviderKeys.createdBy).get(QLUserKeys.id))
    //          .isEqualTo(settingAttribute.getCreatedBy().getUuid());
    //    }
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryCloudProviders() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());

    final SettingAttribute aws = settingGenerator.ensureAwsTest(seed, owners);
    assertThat(aws).isNotNull();
    final SettingAttribute physical = settingGenerator.ensurePhysicalDataCenter(seed, owners);
    assertThat(physical).isNotNull();
    final SettingAttribute azure = settingGenerator.ensureAzureTestCloudProvider(seed, owners);
    assertThat(azure).isNotNull();

    {
      String query = $GQL(/*
{
  cloudProviders(limit:100) {
    nodes {
      id
      name
    }
  }
}*/);

      QLTestObject qlAwsCP = qlExecute(query, aws.getAccountId());
      assertThat(qlAwsCP.sub(QLCloudProviderConnectionKeys.nodes).size()).isNotEqualTo(0);
    }
  }
}
