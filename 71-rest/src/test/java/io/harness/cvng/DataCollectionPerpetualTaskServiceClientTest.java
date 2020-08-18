package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.perpetualtask.CVDataCollectionInfo;
import io.harness.cvng.perpetualtask.DataCollectionPerpetualTaskServiceClient;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.HashMap;
import java.util.Map;

public class DataCollectionPerpetualTaskServiceClientTest extends WingsBaseTest {
  private DataCollectionPerpetualTaskServiceClient dataCollectionPerpetualTaskServiceClient;
  @Inject private KryoSerializer kryoSerializer;
  private String connectorId;
  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    connectorId = generateUuid();
    dataCollectionPerpetualTaskServiceClient = new DataCollectionPerpetualTaskServiceClient();
    FieldUtils.writeField(dataCollectionPerpetualTaskServiceClient, "kryoSerializer", kryoSerializer, true);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTaskParams() {
    String cvConfigId = generateUuid();
    String accountId = generateUuid();
    EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder().build();
    Map<String, String> clientParamMap = new HashMap<>();
    clientParamMap.put("accountId", accountId);
    clientParamMap.put("cvConfigId", cvConfigId);
    clientParamMap.put("dataCollectionWorkerId", cvConfigId);
    clientParamMap.put("connectorId", connectorId);

    SecretRefData secretRefData = SecretRefData.builder().identifier("secret").scope(Scope.ACCOUNT).build();
    AppDynamicsConnectorDTO connectorDTO = AppDynamicsConnectorDTO.builder()
                                               .username("user")
                                               .accountId(accountId)
                                               .accountname("accName")
                                               .controllerUrl("url")
                                               .passwordRef(secretRefData)
                                               .build();

    DataCollectionConnectorBundle bundle = DataCollectionConnectorBundle.builder()
                                               .connectorConfigDTO(connectorDTO)
                                               .details(Lists.newArrayList(encryptedDataDetail))
                                               .build();
    byte[] bundleBytes = kryoSerializer.asBytes(bundle);
    PerpetualTaskClientContext clientContext =
        PerpetualTaskClientContext.builder().clientParams(clientParamMap).executionBundle(bundleBytes).build();
    DataCollectionPerpetualTaskParams dataCollectionInfo =
        (DataCollectionPerpetualTaskParams) dataCollectionPerpetualTaskServiceClient.getTaskParams(clientContext);
    assertThat(dataCollectionInfo.getAccountId()).isEqualTo(accountId);
    assertThat(dataCollectionInfo.getCvConfigId()).isEqualTo(cvConfigId);
    CVDataCollectionInfo cvDataCollectionInfo = CVDataCollectionInfo.builder()
                                                    .connectorConfigDTO(bundle.getConnectorConfigDTO())
                                                    .encryptedDataDetails(bundle.getDetails())
                                                    .build();
    assertThat(dataCollectionInfo.getDataCollectionInfo())
        .isEqualTo(ByteString.copyFrom(kryoSerializer.asBytes(cvDataCollectionInfo)));
  }
}
