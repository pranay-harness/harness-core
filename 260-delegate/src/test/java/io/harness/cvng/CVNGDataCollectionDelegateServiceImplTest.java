package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.splunk.SplunkSavedSearchRequest;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.apis.dto.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;

import software.wings.service.intfc.cvng.CVNGDataCollectionDelegateService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class CVNGDataCollectionDelegateServiceImplTest extends CategoryTest {
  private CVNGDataCollectionDelegateService cvngDataCollectionDelegateService;
  @Mock private SecretDecryptionService secretDecryptionService;
  private Clock clock;
  @Mock private DataCollectionDSLService dataCollectionDSLService;

  private String accountId;
  @Before
  public void setup() throws IllegalAccessException {
    initMocks(this);
    accountId = generateUuid();
    cvngDataCollectionDelegateService = new CVNGDataCollectionDelegateServiceImpl();
    FieldUtils.writeField(cvngDataCollectionDelegateService, "secretDecryptionService", secretDecryptionService, true);
    clock = Clock.fixed(Instant.parse("2020-04-22T10:02:06Z"), ZoneOffset.UTC);
    FieldUtils.writeField(cvngDataCollectionDelegateService, "clock", clock, true);
    FieldUtils.writeField(
        cvngDataCollectionDelegateService, "dataCollectionDSLService", dataCollectionDSLService, true);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetDataCollectionResult() {
    ConnectorConfigDTO connectorConfigDTO =
        SplunkConnectorDTO.builder()
            .splunkUrl("https://splunk.dev.harness.io:8089/")
            .username("harnessadmin")
            .passwordRef(SecretRefData.builder().decryptedValue("123".toCharArray()).build())
            .build();
    when(dataCollectionDSLService.execute(any(), any(), any())).thenReturn(Collections.singletonMap("a", 1));
    String result = cvngDataCollectionDelegateService.getDataCollectionResult(accountId,
        SplunkSavedSearchRequest.builder()
            .connectorInfoDTO(ConnectorInfoDTO.builder().connectorConfig(connectorConfigDTO).build())
            .build(),
        new ArrayList<>());
    assertThat(result).isEqualTo("{\"a\":1}");
  }
}
