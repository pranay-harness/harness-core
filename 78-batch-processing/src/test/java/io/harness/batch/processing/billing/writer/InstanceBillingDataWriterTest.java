package io.harness.batch.processing.billing.writer;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ActiveProfiles;
import software.wings.beans.instance.HarnessServiceInfo;

import java.util.HashMap;
import java.util.Map;

@ActiveProfiles("test")
@RunWith(MockitoJUnitRunner.class)
public class InstanceBillingDataWriterTest extends CategoryTest {
  private static final String PARENT_RESOURCE_ID = "parent_resource_id";
  private static final String SERVICE_ID = "service_id";
  private static final String APP_ID = "app_id";
  private static final String CLOUD_PROVIDER_ID = "cloud_provider_id";
  private static final String ENV_ID = "env_id";
  private static final String INFRA_MAPPING_ID = "infra_mapping_id";

  @InjectMocks private InstanceBillingDataWriter instanceBillingDataWriter;

  @Test
  @Owner(emails = HITESH)
  @Category(UnitTests.class)
  public void testGetValueForKeyFromInstanceMetaData() {
    Map<String, String> metaData = new HashMap<>();
    metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_ID, PARENT_RESOURCE_ID);
    InstanceData instanceData = InstanceData.builder().metaData(metaData).build();
    String parentInstanceId = instanceBillingDataWriter.getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.PARENT_RESOURCE_ID, instanceData);
    assertThat(parentInstanceId).isEqualTo(PARENT_RESOURCE_ID);
  }

  @Test
  @Owner(emails = HITESH)
  @Category(UnitTests.class)
  public void testGetNullValueForKeyFromInstanceMetaData() {
    InstanceData instanceData = InstanceData.builder().build();
    String parentInstanceId = instanceBillingDataWriter.getValueForKeyFromInstanceMetaData(
        InstanceMetaDataConstants.PARENT_RESOURCE_ID, instanceData);
    assertThat(parentInstanceId).isNull();
  }

  @Test
  @Owner(emails = HITESH)
  @Category(UnitTests.class)
  public void testGetEmptyHarnessServiceInfo() {
    InstanceData instanceData = InstanceData.builder().build();
    HarnessServiceInfo harnessServiceInfo = instanceBillingDataWriter.getHarnessServiceInfo(instanceData);
    assertThat(harnessServiceInfo).isNotNull();
    assertThat(harnessServiceInfo.getAppId()).isNull();
  }

  @Test
  @Owner(emails = HITESH)
  @Category(UnitTests.class)
  public void testGetHarnessServiceInfo() {
    InstanceData instanceData = InstanceData.builder().harnessServiceInfo(getHarnessServiceInfo()).build();
    HarnessServiceInfo harnessServiceInfo = instanceBillingDataWriter.getHarnessServiceInfo(instanceData);
    assertThat(harnessServiceInfo).isNotNull();
    assertThat(harnessServiceInfo.getAppId()).isEqualTo(APP_ID);
  }

  private HarnessServiceInfo getHarnessServiceInfo() {
    return new HarnessServiceInfo(SERVICE_ID, APP_ID, CLOUD_PROVIDER_ID, ENV_ID, INFRA_MAPPING_ID);
  }
}
