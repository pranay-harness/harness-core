package software.wings.service.impl.sumo;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SumoLogicSetupTestNodedataTest {
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHostNameField_whenSourceHost() {
    final SumoLogicSetupTestNodedata sourceHost =
        SumoLogicSetupTestNodedata.builder().hostNameField("_sourceHost").build();
    assertThat(sourceHost.getHostNameField()).isEqualTo("_sourcehost");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHostNameField_whenSourceName() {
    final SumoLogicSetupTestNodedata sourceHost =
        SumoLogicSetupTestNodedata.builder().hostNameField("_sourceName").build();
    assertThat(sourceHost.getHostNameField()).isEqualTo("_sourcename");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetHostNameField() {
    String hostNameField = generateUuid();
    final SumoLogicSetupTestNodedata sourceHost =
        SumoLogicSetupTestNodedata.builder().hostNameField(hostNameField).build();
    assertThat(sourceHost.getHostNameField()).isEqualTo(hostNameField);
  }
}