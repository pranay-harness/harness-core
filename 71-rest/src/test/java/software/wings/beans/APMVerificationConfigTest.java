package software.wings.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SRIRAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.CollectionUtils;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.APMVerificationConfig.KeyValues;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class APMVerificationConfigTest extends WingsBaseTest {
  @Mock SecretManager secretManager;
  @Mock EncryptionService encryptionService;

  @Rule public ExpectedException thrown = ExpectedException.none();
  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void encryptFields() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> headers = new ArrayList<>();
    headers.add(KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    headers.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager, false);

    assertThat(apmVerificationConfig.getHeadersList()).hasSize(2);
    assertThat(apmVerificationConfig.getHeadersList().get(0).getValue()).isEqualTo("*****");
    assertThat(apmVerificationConfig.getHeadersList().get(0).getEncryptedValue()).isEqualTo("xyz");
    assertThat(apmVerificationConfig.getHeadersList().get(1).getValue()).isEqualTo("123");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testEncryptFields_whenSecretRefEnabled() {
    String headerSecretRef = generateUuid();
    String optionSecretRef = generateUuid();
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> headers = new ArrayList<>();
    headers.add(KeyValues.builder().key("api_key").value(headerSecretRef).encrypted(true).build());
    headers.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());

    List<KeyValues> options = new ArrayList<>();
    options.add(KeyValues.builder().key("option_key").value(optionSecretRef).encrypted(true).build());
    options.add(KeyValues.builder().key("option_key_plain").value("321").encrypted(false).build());

    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setOptionsList(options);
    apmVerificationConfig.setAccountId("111");
    apmVerificationConfig.encryptFields(secretManager, true);

    assertThat(apmVerificationConfig.getHeadersList()).hasSize(2);
    assertThat(apmVerificationConfig.getHeadersList().get(0).getValue()).isEqualTo(headerSecretRef);
    assertThat(apmVerificationConfig.getHeadersList().get(0).getEncryptedValue()).isEqualTo(headerSecretRef);
    assertThat(apmVerificationConfig.getHeadersList().get(1).getValue()).isEqualTo("123");

    assertThat(apmVerificationConfig.getOptionsList()).hasSize(2);
    assertThat(apmVerificationConfig.getOptionsList().get(0).getValue()).isEqualTo(optionSecretRef);
    assertThat(apmVerificationConfig.getOptionsList().get(0).getEncryptedValue()).isEqualTo(optionSecretRef);
    assertThat(apmVerificationConfig.getOptionsList().get(1).getValue()).isEqualTo("321");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testFetchRelevantEncryptedSecrets() {
    String headerSecretRef = generateUuid();
    String optionSecretRef = generateUuid();
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> headers = new ArrayList<>();
    headers.add(KeyValues.builder().key("api_key").encryptedValue(headerSecretRef).encrypted(true).build());
    headers.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());

    List<KeyValues> options = new ArrayList<>();
    options.add(KeyValues.builder().key("option_key").encryptedValue(optionSecretRef).encrypted(true).build());
    options.add(KeyValues.builder().key("option_key_plain").value("321").encrypted(false).build());

    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setOptionsList(options);
    apmVerificationConfig.setAccountId("111");

    int numOfUrlSecrets = 5;
    int numOfBodySecrets = 7;

    List<String> urlSecrets = new ArrayList<>();
    String validationUrl = "validatiobUrl?";
    for (int i = 0; i < numOfUrlSecrets; i++) {
      String secretId = generateUuid();
      validationUrl += "&token" + i + "=${secretRef:" + generateUuid() + "," + secretId + "}";
      urlSecrets.add(secretId);
    }
    apmVerificationConfig.setValidationUrl(validationUrl);

    List<String> bodySecrets = new ArrayList<>();
    String validationBody = "{body}";
    for (int i = 0; i < numOfBodySecrets; i++) {
      String secretId = generateUuid();
      validationBody += "&token" + i + "=${secretRef:" + generateUuid() + "," + secretId + "}";
      bodySecrets.add(secretId);
    }
    apmVerificationConfig.setValidationBody(validationBody);

    List<String> expectedRelevantSecretIds = new ArrayList<>();
    expectedRelevantSecretIds.add(headerSecretRef);
    expectedRelevantSecretIds.add(optionSecretRef);
    expectedRelevantSecretIds.addAll(urlSecrets);
    expectedRelevantSecretIds.addAll(bodySecrets);

    assertThat(CollectionUtils.isEqualCollection(
                   expectedRelevantSecretIds, apmVerificationConfig.fetchRelevantEncryptedSecrets()))
        .isTrue();
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void encryptFieldsMasked() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> headers = new ArrayList<>();
    headers.add(KeyValues.builder().key("api_key").value("*****").encrypted(true).encryptedValue("xyz").build());
    headers.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager, false);
    verifyZeroInteractions(secretManager);
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void encryptDataDetails() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> headers = new ArrayList<>();
    headers.add(KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    headers.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager, false);

    when(secretManager.encryptedDataDetails("111", "api_key", "xyz"))
        .thenReturn(Optional.of(EncryptedDataDetail.builder().fieldName("api_key").build()));
    List<EncryptedDataDetail> encryptedDataDetails = apmVerificationConfig.encryptedDataDetails(secretManager);
    assertThat(encryptedDataDetails).hasSize(1);
    assertThat(encryptedDataDetails.isEmpty()).isFalse();
    assertThat(encryptedDataDetails.get(0).getFieldName()).isEqualTo("api_key");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void encryptFieldsParams() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> params = new ArrayList<>();
    params.add(KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    params.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setOptionsList(params);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager, false);

    assertThat(apmVerificationConfig.getOptionsList()).hasSize(2);
    assertThat(apmVerificationConfig.getOptionsList().get(0).getValue()).isEqualTo("*****");
    assertThat(apmVerificationConfig.getOptionsList().get(0).getEncryptedValue()).isEqualTo("xyz");
    assertThat(apmVerificationConfig.getOptionsList().get(1).getValue()).isEqualTo("123");
    assertThat(apmVerificationConfig.getOptionsList().get(1).getEncryptedValue()).isEqualTo(null);
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void encryptDataDetailsParams() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> params = new ArrayList<>();
    params.add(KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    params.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    apmVerificationConfig.setOptionsList(params);
    apmVerificationConfig.setAccountId("111");
    when(secretManager.encrypt("111", "123", null)).thenReturn("xyz");
    apmVerificationConfig.encryptFields(secretManager, false);

    when(secretManager.encryptedDataDetails("111", "api_key", "xyz"))
        .thenReturn(Optional.of(EncryptedDataDetail.builder().fieldName("api_key").build()));
    List<EncryptedDataDetail> encryptedDataDetails = apmVerificationConfig.encryptedDataDetails(secretManager);
    assertThat(encryptedDataDetails).hasSize(1);
    assertThat(encryptedDataDetails.isEmpty()).isFalse();
    assertThat(encryptedDataDetails.get(0).getFieldName()).isEqualTo("api_key");
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void createAPMValidateCollectorConfig() throws IOException {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> headers = new ArrayList<>();
    headers.add(KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    headers.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
    headers.add(KeyValues.builder().key("api_key_2").value("*****").encryptedValue("abc").encrypted(true).build());

    Optional<EncryptedDataDetail> encryptedDataDetail =
        Optional.of(EncryptedDataDetail.builder().fieldName("api_key_2").build());

    when(secretManager.encryptedDataDetails("111", "api_key_2", "abc")).thenReturn(encryptedDataDetail);
    when(encryptionService.getDecryptedValue(encryptedDataDetail.get())).thenReturn("abc".toCharArray());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setAccountId("111");
    apmVerificationConfig.setUrl("base");
    apmVerificationConfig.setValidationUrl("suffix");
    APMValidateCollectorConfig apmValidateCollectorConfig =
        apmVerificationConfig.createAPMValidateCollectorConfig(secretManager, encryptionService, false);
    assertThat(apmValidateCollectorConfig.getBaseUrl()).isEqualTo("base");
    assertThat(apmValidateCollectorConfig.getUrl()).isEqualTo("suffix");
    assertThat(apmVerificationConfig.getHeadersList()).isEqualTo(headers);
    assertThat(apmValidateCollectorConfig.getHeaders().get("api_key_2")).isEqualTo("abc");
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreateAPMValidateCollectorConfig_whenSecretRefEnabled() throws IOException {
    String headerSecretRef = generateUuid();
    String optionSecretRef = generateUuid();
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> headers = new ArrayList<>();
    headers.add(KeyValues.builder().key("api_key").value(headerSecretRef).encrypted(true).build());
    headers.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());

    List<KeyValues> options = new ArrayList<>();
    options.add(KeyValues.builder().key("option_key").value(optionSecretRef).encrypted(true).build());
    options.add(KeyValues.builder().key("option_key_plain").value("321").encrypted(false).build());
    Optional<EncryptedDataDetail> headerEncryptedDataDetail =
        Optional.of(EncryptedDataDetail.builder().fieldName("api_key_2").build());
    Optional<EncryptedDataDetail> optionEncryptedDataDetail =
        Optional.of(EncryptedDataDetail.builder().fieldName("option_key_2").build());

    when(secretManager.encryptedDataDetails("111", "api_key", headerSecretRef)).thenReturn(headerEncryptedDataDetail);
    when(secretManager.encryptedDataDetails("111", "option_key", optionSecretRef))
        .thenReturn(optionEncryptedDataDetail);
    when(encryptionService.getDecryptedValue(headerEncryptedDataDetail.get()))
        .thenReturn("decryptedHeader".toCharArray());
    when(encryptionService.getDecryptedValue(optionEncryptedDataDetail.get()))
        .thenReturn("decryptedOption".toCharArray());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setOptionsList(options);
    apmVerificationConfig.setAccountId("111");
    apmVerificationConfig.setUrl("base");
    apmVerificationConfig.setValidationUrl("suffix");
    APMValidateCollectorConfig apmValidateCollectorConfig =
        apmVerificationConfig.createAPMValidateCollectorConfig(secretManager, encryptionService, true);
    assertThat(apmValidateCollectorConfig.getBaseUrl()).isEqualTo("base");
    assertThat(apmValidateCollectorConfig.getUrl()).isEqualTo("suffix");

    Map<String, String> expeectedHeaders = new HashMap<>();
    expeectedHeaders.put("api_key", "decryptedHeader");
    expeectedHeaders.put("api_key_plain", "123");

    Map<String, String> expectedOptions = new HashMap<>();
    expectedOptions.put("option_key", "decryptedOption");
    expectedOptions.put("option_key_plain", "321");

    assertThat(apmValidateCollectorConfig.getHeaders()).isEqualTo(expeectedHeaders);
    assertThat(apmValidateCollectorConfig.getOptions()).isEqualTo(expectedOptions);
  }

  @Test
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void collectionHeaders() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> headers = new ArrayList<>();
    headers.add(KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    headers.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
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
  @Owner(developers = SRIRAM)
  @Category(UnitTests.class)
  public void collectionParams() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    List<KeyValues> params = new ArrayList<>();
    params.add(KeyValues.builder().key("api_key").value("123").encrypted(true).build());
    params.add(KeyValues.builder().key("api_key_plain").value("123").encrypted(false).build());
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
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetValidationUrlEncoded() {
    APMVerificationConfig apmVerificationConfig = new APMVerificationConfig();
    apmVerificationConfig.setValidationUrl("`requestwithbacktick`");
    assertThat(apmVerificationConfig.getValidationUrl()).isEqualTo("%60requestwithbacktick%60");
  }
}
