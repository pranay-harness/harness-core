package software.wings.service.impl.elk;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.ElkConfig;
import software.wings.beans.SettingAttribute;
import software.wings.common.VerificationConstants;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.elk.ElkDelegateService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ElkAnalysisServiceImplTest extends WingsBaseTest {
  @InjectMocks ElkAnalysisServiceImpl service;
  @Mock SettingsService settingService;
  @Mock SecretManager secretManager;
  @Mock DelegateProxyFactory delegateProxyFactory;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLogDataByHostForTotalHitsAndThreshold() throws IOException {
    String accountId = generateUuid();
    String appId = generateUuid();
    String settingId = generateUuid();
    ElkSetupTestNodeData elkSetupTestNodeData = ElkSetupTestNodeData.builder()
                                                    .settingId(settingId)
                                                    .appId(appId)
                                                    .isServiceLevel(true)
                                                    .timeStampFieldFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
                                                    .build();
    List<EncryptedDataDetail> encryptedDataDetails = mock(List.class);
    SettingAttribute settingAttribute = mock(SettingAttribute.class);
    ElkConfig elkConfig = mock(ElkConfig.class);
    when(settingService.get(eq(settingId))).thenReturn(settingAttribute);
    when(settingAttribute.getValue()).thenReturn(elkConfig);
    when(secretManager.getEncryptionDetails(anyObject(), eq(appId), anyString())).thenReturn(encryptedDataDetails);
    ElkDelegateService elkDelegateService = mock(ElkDelegateService.class);
    when(delegateProxyFactory.get(eq(ElkDelegateService.class), anyObject())).thenReturn(elkDelegateService);
    Object elkResponse = getELKResponse(15 * 201);
    when(elkDelegateService.search(any(), any(), any(), any(), anyInt())).thenReturn(elkResponse);

    VerificationNodeDataSetupResponse verificationNodeDataSetupResponse =
        service.getLogDataByHost(accountId, elkSetupTestNodeData);
    assertEquals(201, verificationNodeDataSetupResponse.getLoadResponse().getTotalHits());
    assertEquals(VerificationConstants.TOTAL_HITS_PER_MIN_THRESHOLD,
        verificationNodeDataSetupResponse.getLoadResponse().getTotalHitsThreshold());
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateQueryPassesIfTotalHitsIsLessThenTheThreshold() throws IOException {
    String accountId = generateUuid();
    String appId = generateUuid();
    String settingId = generateUuid();
    String query = generateUuid();
    List<EncryptedDataDetail> encryptedDataDetails = mock(List.class);
    SettingAttribute settingAttribute = mock(SettingAttribute.class);
    ElkConfig elkConfig = mock(ElkConfig.class);
    when(settingService.get(eq(settingId))).thenReturn(settingAttribute);
    when(settingAttribute.getValue()).thenReturn(elkConfig);
    when(secretManager.getEncryptionDetails(anyObject(), eq(appId), anyString())).thenReturn(encryptedDataDetails);
    ElkDelegateService elkDelegateService = mock(ElkDelegateService.class);
    when(delegateProxyFactory.get(eq(ElkDelegateService.class), anyObject())).thenReturn(elkDelegateService);
    Object responseWithoutHost = getELKResponse(15 * 20);
    when(elkDelegateService.search(any(), any(), any(), any(), anyInt())).thenReturn(responseWithoutHost);

    assertEquals(
        true, service.validateQuery(accountId, appId, settingId, query, "test", null, "hostname", "log", "@timestamp"));
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateQueryThrowsWingsExceptionIfTotalHitsIsLessThenTheThreshold() throws IOException {
    String accountId = generateUuid();
    String appId = generateUuid();
    String settingId = generateUuid();
    String query = generateUuid();
    List<EncryptedDataDetail> encryptedDataDetails = mock(List.class);
    SettingAttribute settingAttribute = mock(SettingAttribute.class);
    ElkConfig elkConfig = mock(ElkConfig.class);
    when(settingService.get(eq(settingId))).thenReturn(settingAttribute);
    when(settingAttribute.getValue()).thenReturn(elkConfig);
    when(secretManager.getEncryptionDetails(anyObject(), eq(appId), anyString())).thenReturn(encryptedDataDetails);
    ElkDelegateService elkDelegateService = mock(ElkDelegateService.class);
    when(delegateProxyFactory.get(eq(ElkDelegateService.class), anyObject())).thenReturn(elkDelegateService);
    Object responseWithoutHost = getELKResponse(1000 * 15 + 1); // per minute 1000
    when(elkDelegateService.search(any(), any(), any(), any(), anyInt())).thenReturn(responseWithoutHost);

    try {
      service.validateQuery(accountId, appId, settingId, query, "test", null, "hostname", "log", "@timestamp");
      fail("validate query should throw wings exception..");
    } catch (WingsException e) {
      assertEquals(e.getParams().get("reason"),
          "Error in Elasticsearch configuration. Too many logs returned using query: '" + query
              + "'. Please refine your query.");
      assertEquals(ErrorCode.ELK_CONFIGURATION_ERROR, e.getCode());
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateQueryThrowsWingsExceptionIfSearchThrowsRuntimeException() throws IOException {
    String accountId = generateUuid();
    String appId = generateUuid();
    String settingId = generateUuid();
    String query = generateUuid();
    List<EncryptedDataDetail> encryptedDataDetails = mock(List.class);
    SettingAttribute settingAttribute = mock(SettingAttribute.class);
    ElkConfig elkConfig = mock(ElkConfig.class);
    when(settingService.get(eq(settingId))).thenReturn(settingAttribute);
    when(settingAttribute.getValue()).thenReturn(elkConfig);
    when(secretManager.getEncryptionDetails(anyObject(), eq(appId), anyString())).thenReturn(encryptedDataDetails);
    ElkDelegateService elkDelegateService = mock(ElkDelegateService.class);
    when(delegateProxyFactory.get(eq(ElkDelegateService.class), anyObject())).thenReturn(elkDelegateService);
    when(elkDelegateService.search(any(), any(), any(), any(), anyInt()))
        .thenThrow(new RuntimeException("Search failed from test...."));

    try {
      service.validateQuery(accountId, appId, settingId, query, "test", null, "hostname", "log", "@timestamp");
      fail("validate query should throw wings exception..");
    } catch (WingsException e) {
      assertEquals(ErrorCode.ELK_CONFIGURATION_ERROR, e.getCode());
    }
  }

  private Object getELKResponse(int total) {
    String resp = "{\n"
        + "  \"took\": 20,\n"
        + "  \"timed_out\": false,\n"
        + "  \"_shards\": {\n"
        + "    \"total\": 275,\n"
        + "    \"successful\": 275,\n"
        + "    \"skipped\": 270,\n"
        + "    \"failed\": 0\n"
        + "  },\n"
        + "  \"hits\": {\n"
        + "    \"total\": " + total + ",\n"
        + "    \"max_score\": 0,\n"
        + "    \"hits\": [\n"
        + "    ]\n"
        + "  }\n"
        + "}";
    return JsonUtils.asObject(resp, Map.class);
  }
}
