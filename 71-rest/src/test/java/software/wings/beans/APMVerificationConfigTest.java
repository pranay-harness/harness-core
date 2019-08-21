package software.wings.beans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class APMVerificationConfigTest extends WingsBaseTest {
  @Mock SecretManager secretManager;
  @Mock EncryptionService encryptionService;

  @Rule public ExpectedException thrown = ExpectedException.none();
  @Test
  @Category(UnitTests.class)
  public void encryptFields() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<APMVerificationConfig.KeyValues> headers = new ArrayList<>();
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager);

    assertEquals(2, apmVerificationConfig.getHeadersList().size());
    assertEquals("*****", apmVerificationConfig.getHeadersList().get(0).getValue());
    assertEquals("xyz", apmVerificationConfig.getHeadersList().get(0).getEncryptedValue());
    assertEquals("123", apmVerificationConfig.getHeadersList().get(1).getValue());
  }

  @Test
  @Category(UnitTests.class)
  public void encryptFieldsMasked() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<APMVerificationConfig.KeyValues> headers = new ArrayList<>();
    headers.add(APMVerificationConfig.KeyValues.builder()
                    .key("api_key")
                    .value("*****")
                    .encrypted(true)
                    .encryptedValue("xyz")
                    .build());
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager);
    verifyZeroInteractions(secretManager);
  }

  @Test
  @Category(UnitTests.class)
  public void encryptDataDetails() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<APMVerificationConfig.KeyValues> headers = new ArrayList<>();
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager);

    when(secretManager.encryptedDataDetails("111", "api_key", "xyz"))
        .thenReturn(Optional.of(EncryptedDataDetail.builder().fieldName("api_key").build()));
    List<EncryptedDataDetail> encryptedDataDetails = apmVerificationConfig.encryptedDataDetails(secretManager);
    assertEquals(1, encryptedDataDetails.size());
    assertThat(encryptedDataDetails.isEmpty()).isFalse();
    assertEquals("api_key", encryptedDataDetails.get(0).getFieldName());
  }

  @Test
  @Category(UnitTests.class)
  public void encryptFieldsParams() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<APMVerificationConfig.KeyValues> params = new ArrayList<>();
    params.add(APMVerificationConfig.KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    params.add(APMVerificationConfig.KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setOptionsList(params);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager);

    assertEquals(2, apmVerificationConfig.getOptionsList().size());
    assertEquals("*****", apmVerificationConfig.getOptionsList().get(0).getValue());
    assertEquals("xyz", apmVerificationConfig.getOptionsList().get(0).getEncryptedValue());
    assertEquals("123", apmVerificationConfig.getOptionsList().get(1).getValue());
    assertEquals(null, apmVerificationConfig.getOptionsList().get(1).getEncryptedValue());
  }

  @Test
  @Category(UnitTests.class)
  public void encryptDataDetailsParams() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<APMVerificationConfig.KeyValues> params = new ArrayList<>();
    params.add(APMVerificationConfig.KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    params.add(APMVerificationConfig.KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setOptionsList(params);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager);

    when(secretManager.encryptedDataDetails("111", "api_key", "xyz"))
        .thenReturn(Optional.of(EncryptedDataDetail.builder().fieldName("api_key").build()));
    List<EncryptedDataDetail> encryptedDataDetails = apmVerificationConfig.encryptedDataDetails(secretManager);
    assertEquals(1, encryptedDataDetails.size());
    assertThat(encryptedDataDetails.isEmpty()).isFalse();
    assertEquals("api_key", encryptedDataDetails.get(0).getFieldName());
  }

  @Test
  @Category(UnitTests.class)
  public void createAPMValidateCollectorConfig() throws IOException {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<APMVerificationConfig.KeyValues> headers = new ArrayList<>();
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    headers.add(APMVerificationConfig.KeyValues.builder()
                    .key("api_key_2")
                    .value("*****")
                    .encryptedValue("abc")
                    .encrypted(true)
                    .build());

    Optional<EncryptedDataDetail> encryptedDataDetail =
        Optional.of(EncryptedDataDetail.builder().fieldName("api_key_2").build());

    when(secretManager.encryptedDataDetails("111", "api_key_2", "abc")).thenReturn(encryptedDataDetail);
    when(encryptionService.getDecryptedValue(encryptedDataDetail.get())).thenReturn("abc".toCharArray());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    apmVerificationConfig.setUrl("base");
    apmVerificationConfig.setValidationUrl("suffix");
    APMValidateCollectorConfig apmValidateCollectorConfig =
        apmVerificationConfig.createAPMValidateCollectorConfig(secretManager, encryptionService);
    assertEquals("base", apmValidateCollectorConfig.getBaseUrl());
    assertEquals("suffix", apmValidateCollectorConfig.getUrl());
    assertEquals(headers, apmVerificationConfig.getHeadersList());
    assertEquals("abc", apmValidateCollectorConfig.getHeaders().get("api_key_2"));
  }

  @Test
  @Category(UnitTests.class)
  public void createAPMValidateCollectorConfigEmptyURL() throws IOException {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<APMVerificationConfig.KeyValues> headers = new ArrayList<>();
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    headers.add(APMVerificationConfig.KeyValues.builder()
                    .key("api_key_2")
                    .value("*****")
                    .encryptedValue("abc")
                    .encrypted(true)
                    .build());

    Optional<EncryptedDataDetail> encryptedDataDetail =
        Optional.of(EncryptedDataDetail.builder().fieldName("api_key_2").build());

    when(secretManager.encryptedDataDetails("111", "api_key_2", "abc")).thenReturn(encryptedDataDetail);
    when(encryptionService.getDecryptedValue(encryptedDataDetail.get())).thenReturn("abc".toCharArray());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    apmVerificationConfig.setUrl("base");
    thrown.expect(WingsException.class);
    apmVerificationConfig.createAPMValidateCollectorConfig(secretManager, encryptionService);
  }

  @Test
  @Category(UnitTests.class)
  public void collectionHeaders() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<APMVerificationConfig.KeyValues> headers = new ArrayList<>();
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    headers.add(APMVerificationConfig.KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    apmVerificationConfig.setUrl("base");
    apmVerificationConfig.setValidationUrl("suffix");
    Map<String, String> collectionHeaders = apmVerificationConfig.collectionHeaders();
    assertEquals("${api_key}", collectionHeaders.get("api_key"));
    assertEquals("123", collectionHeaders.get("api_key_plain"));
    assertEquals(2, collectionHeaders.size());
  }

  @Test
  @Category(UnitTests.class)
  public void collectionParams() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<APMVerificationConfig.KeyValues> params = new ArrayList<>();
    params.add(APMVerificationConfig.KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    params.add(APMVerificationConfig.KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setOptionsList(params);
    apmVerificationConfig.setAccountId("111");
    apmVerificationConfig.setUrl("base");
    apmVerificationConfig.setValidationUrl("suffix");
    Map<String, String> collectionParams = apmVerificationConfig.collectionParams();
    assertEquals("${api_key}", collectionParams.get("api_key"));
    assertEquals("123", collectionParams.get("api_key_plain"));
    assertEquals(2, collectionParams.size());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetValidationUrlEncoded() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    apmVerificationConfig.setValidationUrl("`requestwithbacktick`");
    assertEquals("Back tick should be encoded", "%60requestwithbacktick%60", apmVerificationConfig.getValidationUrl());
  }
}
