package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;

import io.harness.category.element.UnitTests;
import io.harness.cvng.perpetualtask.CVDataCollectionInfo;
import io.harness.cvng.perpetualtask.DataCollectionPerpetualTaskServiceClient;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SplunkConfig;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.Map;

public class DataCollectionPerpetualTaskServiceClientTest extends WingsBaseTest {
  private DataCollectionPerpetualTaskServiceClient dataCollectionPerpetualTaskServiceClient;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  private String connectorId;
  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    connectorId = generateUuid();
    dataCollectionPerpetualTaskServiceClient = new DataCollectionPerpetualTaskServiceClient();
    FieldUtils.writeField(dataCollectionPerpetualTaskServiceClient, "settingsService", settingsService, true);
    FieldUtils.writeField(dataCollectionPerpetualTaskServiceClient, "secretManager", secretManager, true);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getTaskParams() {
    String cvConfigId = generateUuid();
    SplunkConfig splunkConfig = SplunkConfig.builder().username("user").encryptedPassword("pass").build();
    when(settingsService.get(eq(connectorId)))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withValue(splunkConfig).build());
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    when(secretManager.getEncryptionDetails(eq(splunkConfig))).thenReturn(Lists.newArrayList(encryptedDataDetail));
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put("cvConfigId", cvConfigId);
    clientParamMap.put("connectorId", connectorId);
    PerpetualTaskClientContext clientContext = new PerpetualTaskClientContext(clientParamMap);
    DataCollectionPerpetualTaskParams dataCollectionInfo =
        (DataCollectionPerpetualTaskParams) dataCollectionPerpetualTaskServiceClient.getTaskParams(clientContext);
    assertThat(dataCollectionInfo.getCvConfigId()).isEqualTo(cvConfigId);
    CVDataCollectionInfo cvDataCollectionInfo = CVDataCollectionInfo.builder()
                                                    .settingValue(splunkConfig)
                                                    .encryptedDataDetails(Lists.newArrayList(encryptedDataDetail))
                                                    .build();
    assertThat(dataCollectionInfo.getDataCollectionInfo())
        .isEqualTo(ByteString.copyFrom(KryoUtils.asBytes(cvDataCollectionInfo)));
  }
}