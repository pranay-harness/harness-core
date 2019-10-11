package io.harness.pcf;

import static io.harness.pcf.ManifestType.APPLICATION_MANIFEST;
import static io.harness.pcf.ManifestType.APPLICATION_MANIFEST_WITH_CREATE_SERVICE;
import static io.harness.pcf.ManifestType.CREATE_SERVICE_MANIFEST;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PcfFileTypeCheckerTest extends CategoryTest {
  private String MANIFEST_YML = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : 2\n"
      + "  random-route: true\n";

  private String MANIFEST_YML_CREATE_SERVICE_PUSH_YML = "applications:\n"
      + "- name: ((PCF_APP_NAME))\n"
      + "  memory: ((PCF_APP_MEMORY))\n"
      + "  instances : 2\n"
      + "  random-route: true\n"
      + "create-services:\n"
      + "- name:   \"my-database-service\"\n"
      + "  broker: \"p-mysql\"\n"
      + "  plan:   \"1gb\"\n"
      + "\n"
      + "- name:   \"Another-service\"\n"
      + "  broker: \"p-brokerName\"\n"
      + "  plan:   \"sharedPlan\"\n"
      + "  parameters: \"{\\\"RAM\\\": 4gb }\"";

  private String CREATE_SERVICE_PUSH_YML = "create-services:\n"
      + "- name:   \"my-database-service\"\n"
      + "  broker: \"p-mysql\"\n"
      + "  plan:   \"1gb\"\n"
      + "\n"
      + "- name:   \"Another-service\"\n"
      + "  broker: \"p-brokerName\"\n"
      + "  plan:   \"sharedPlan\"\n"
      + "  parameters: \"{\\\"RAM\\\": 4gb }\"";

  @Test
  @Category(UnitTests.class)
  public void testCategory() {
    PcfFileTypeChecker pcfFileTypeChecker = new PcfFileTypeChecker();
    assertThat(pcfFileTypeChecker.getManifestType(MANIFEST_YML)).isEqualTo(APPLICATION_MANIFEST);
    assertThat(pcfFileTypeChecker.getManifestType(MANIFEST_YML_CREATE_SERVICE_PUSH_YML))
        .isEqualTo(APPLICATION_MANIFEST_WITH_CREATE_SERVICE);
    assertThat(pcfFileTypeChecker.getManifestType(CREATE_SERVICE_PUSH_YML)).isEqualTo(CREATE_SERVICE_MANIFEST);
  }
}
