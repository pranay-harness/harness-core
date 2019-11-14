package software.wings.beans;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
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
  @Owner(developers = UNKNOWN)
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

    assertThat(apmVerificationConfig.getHeadersList()).hasSize(2);
    assertThat(apmVerificationConfig.getHeadersList().get(0).getValue()).isEqualTo("*****");
    assertThat(apmVerificationConfig.getHeadersList().get(0).getEncryptedValue()).isEqualTo("xyz");
    assertThat(apmVerificationConfig.getHeadersList().get(1).getValue()).isEqualTo("123");
  }

  @Test
  @Owner(developers = UNKNOWN)
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
  @Owner(developers = UNKNOWN)
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
    assertThat(encryptedDataDetails).hasSize(1);
    assertThat(encryptedDataDetails.isEmpty()).isFalse();
    assertThat(encryptedDataDetails.get(0).getFieldName()).isEqualTo("api_key");
  }

  @Test
  @Owner(developers = UNKNOWN)
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

    assertThat(apmVerificationConfig.getOptionsList()).hasSize(2);
    assertThat(apmVerificationConfig.getOptionsList().get(0).getValue()).isEqualTo("*****");
    assertThat(apmVerificationConfig.getOptionsList().get(0).getEncryptedValue()).isEqualTo("xyz");
    assertThat(apmVerificationConfig.getOptionsList().get(1).getValue()).isEqualTo("123");
    assertThat(apmVerificationConfig.getOptionsList().get(1).getEncryptedValue()).isEqualTo(null);
  }

  @Test
  @Owner(developers = UNKNOWN)
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
    assertThat(encryptedDataDetails).hasSize(1);
    assertThat(encryptedDataDetails.isEmpty()).isFalse();
    assertThat(encryptedDataDetails.get(0).getFieldName()).isEqualTo("api_key");
  }

  @Test
  @Owner(developers = UNKNOWN)
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
    assertThat(apmValidateCollectorConfig.getBaseUrl()).isEqualTo("base");
    assertThat(apmValidateCollectorConfig.getUrl()).isEqualTo("suffix");
    assertThat(apmVerificationConfig.getHeadersList()).isEqualTo(headers);
    assertThat(apmValidateCollectorConfig.getHeaders().get("api_key_2")).isEqualTo("abc");
  }

  @Test
  @Owner(developers = UNKNOWN)
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
    assertThat(collectionHeaders.get("api_key")).isEqualTo("${api_key}");
    assertThat(collectionHeaders.get("api_key_plain")).isEqualTo("123");
    assertThat(collectionHeaders).hasSize(2);
  }

  @Test
  @Owner(developers = UNKNOWN)
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
    assertThat(collectionParams.get("api_key")).isEqualTo("${api_key}");
    assertThat(collectionParams.get("api_key_plain")).isEqualTo("123");
    assertThat(collectionParams).hasSize(2);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testGetValidationUrlEncoded() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    apmVerificationConfig.setValidationUrl("`requestwithbacktick`");
    assertThat(apmVerificationConfig.getValidationUrl()).isEqualTo("%60requestwithbacktick%60");
  }
}
