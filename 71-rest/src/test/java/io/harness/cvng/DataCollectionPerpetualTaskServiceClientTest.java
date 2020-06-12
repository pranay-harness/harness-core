package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.HashMap;
import java.util.Map;

public class DataCollectionPerpetualTaskServiceClientTest extends WingsBaseTest {
  private DataCollectionPerpetualTaskServiceClient dataCollectionPerpetualTaskServiceClient;
  @Before
  public void setup() {
    dataCollectionPerpetualTaskServiceClient = new DataCollectionPerpetualTaskServiceClient();
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void create() {
    assertThatThrownBy(() -> dataCollectionPerpetualTaskServiceClient.create(generateUuid(), null))
        .hasMessage("This is implemented in the service layer.");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getTaskParams() {
    String cvConfigId = generateUuid();
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put("cvConfigId", cvConfigId);
    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);
    DataCollectionPerpetualTaskParams dataCollectionInfo =
        (DataCollectionPerpetualTaskParams) dataCollectionPerpetualTaskServiceClient.getTaskParams(clientContext);
    assertThat(dataCollectionInfo.getCvConfigId()).isEqualTo(cvConfigId);
  }
}