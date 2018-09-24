package software.wings.service.impl.analysis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.APMFetchConfig;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.SettingsService;

/**
 * @author Praveen 9/6/18
 */
public class APMVerificationServiceImplTest {
  @Mock private SettingsService mockSettingsService;
  @Mock private DelegateProxyFactory mockDelegateProxyFactory;
  @Mock private APMDelegateService mockDelegateService;
  @InjectMocks APMVerificationServiceImpl service;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGetNodeDataValidCase() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{ 'key1':'value1'}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockDelegateService);
    when(mockDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenReturn(dummyResponseString);

    // execute
    VerificationNodeDataSetupResponse response =
        service.getMetricsWithDataForNode("accountId", "serverConfigId", fetchConfig);

    // verify
    assertNotNull(response);
    assertTrue(response.getLoadResponse().isLoadPresent());
  }

  @Test
  public void testGetNodeDataValidNoLoad() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockDelegateService);
    when(mockDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenReturn(dummyResponseString);

    // execute
    VerificationNodeDataSetupResponse response =
        service.getMetricsWithDataForNode("accountId", "serverConfigId", fetchConfig);

    // verify
    assertNotNull(response);
    assertFalse(response.getLoadResponse().isLoadPresent());
  }

  @Test(expected = WingsException.class)
  public void testGetNodeDataNullServerConfigId() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockDelegateService);
    when(mockDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenReturn(dummyResponseString);

    // execute
    VerificationNodeDataSetupResponse response = service.getMetricsWithDataForNode("accountId", null, fetchConfig);
  }

  @Test(expected = WingsException.class)
  public void testGetNodeDataNullFetchConfig() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockDelegateService);
    when(mockDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenReturn(dummyResponseString);

    // execute
    VerificationNodeDataSetupResponse response = service.getMetricsWithDataForNode("accountId", "serverId", null);
  }

  @Test(expected = WingsException.class)
  public void testGetNodeDataExceptionWhileFetch() {
    APMVerificationConfig config = new APMVerificationConfig();
    config.setValidationUrl("this is a testurl");
    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(config);

    String dummyResponseString = "{}";

    APMFetchConfig fetchConfig = APMFetchConfig.builder().url("testFetchURL.com").build();

    // setup
    when(mockSettingsService.get(anyString())).thenReturn(attribute);
    when(mockDelegateProxyFactory.get(any(), any())).thenReturn(mockDelegateService);
    when(mockDelegateService.fetch(any(APMValidateCollectorConfig.class))).thenThrow(new WingsException(""));

    // execute
    VerificationNodeDataSetupResponse response = service.getMetricsWithDataForNode("accountId", "serverId", null);
  }
}
