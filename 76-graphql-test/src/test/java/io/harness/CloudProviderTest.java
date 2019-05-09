package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.graphql.schema.type.cloudProvider.QLAwsCloudProvider.QLCloudProviderKeys;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.testframework.graphql.QLTestObject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLUser.QLUserKeys;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderConnection.QLCloudProviderConnectionKeys;

@Slf4j
public class CloudProviderTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject private SettingGenerator settingGenerator;

  @Test
  @Category({GraphQLTests.class, UnitTests.class})
  public void testQueryCloudProvider() {
    final Seed seed = new Seed(0);
    final Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());
    {
      final SettingAttribute settingAttribute = settingGenerator.ensureAwsTest(seed, owners);
      assertThat(settingAttribute).isNotNull();

      String query = "{cloudProvider(cloudProviderId: \"" + settingAttribute.getUuid()
          + "\") { id name createdAt createdBy { id }}}";

      QLTestObject qlAwsCP = qlExecute(query);
      assertThat(qlAwsCP.get(QLCloudProviderKeys.id)).isEqualTo(settingAttribute.getUuid());
      assertThat(qlAwsCP.get(QLCloudProviderKeys.name)).isEqualTo(settingAttribute.getName());
      assertThat(qlAwsCP.get(QLCloudProviderKeys.createdAt))
          .isEqualTo(GraphQLDateTimeScalar.convertToString(settingAttribute.getCreatedAt()));
      assertThat(qlAwsCP.sub(QLCloudProviderKeys.createdBy).get(QLUserKeys.id))
          .isEqualTo(settingAttribute.getCreatedBy().getUuid());
    }

    {
      final SettingAttribute settingAttribute = settingGenerator.ensurePhysicalDataCenter(seed, owners);
      assertThat(settingAttribute).isNotNull();

      String query = "{cloudProvider(cloudProviderId: \"" + settingAttribute.getUuid()
          + "\") { id name createdAt createdBy { id }}}";

      QLTestObject qlAwsCP = qlExecute(query);
      assertThat(qlAwsCP.get(QLCloudProviderKeys.id)).isEqualTo(settingAttribute.getUuid());
      assertThat(qlAwsCP.get(QLCloudProviderKeys.name)).isEqualTo(settingAttribute.getName());
      assertThat(qlAwsCP.get(QLCloudProviderKeys.createdAt))
          .isEqualTo(GraphQLDateTimeScalar.convertToString(settingAttribute.getCreatedAt()));
      assertThat(qlAwsCP.sub(QLCloudProviderKeys.createdBy).get(QLUserKeys.id))
          .isEqualTo(settingAttribute.getCreatedBy().getUuid());
    }

    {
      final SettingAttribute settingAttribute = settingGenerator.ensureAzureTestCloudProvider(seed, owners);
      assertThat(settingAttribute).isNotNull();

      String query = "{cloudProvider(cloudProviderId: \"" + settingAttribute.getUuid()
          + "\") { id name createdAt createdBy { id }}}";

      QLTestObject qlAwsCP = qlExecute(query);
      assertThat(qlAwsCP.get(QLCloudProviderKeys.id)).isEqualTo(settingAttribute.getUuid());
      assertThat(qlAwsCP.get(QLCloudProviderKeys.name)).isEqualTo(settingAttribute.getName());
      assertThat(qlAwsCP.get(QLCloudProviderKeys.createdAt))
          .isEqualTo(GraphQLDateTimeScalar.convertToString(settingAttribute.getCreatedAt()));
      assertThat(qlAwsCP.sub(QLCloudProviderKeys.createdBy).get(QLUserKeys.id))
          .isEqualTo(settingAttribute.getCreatedBy().getUuid());
    }
  }

  @Test
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
      String query = "{ cloudProviders(accountId: \"" + aws.getAccountId() + "\") { nodes { id name} } }";

      QLTestObject qlAwsCP = qlExecute(query);
      assertThat(qlAwsCP.sub(QLCloudProviderConnectionKeys.nodes).size()).isNotEqualTo(0);
    }
  }
}
