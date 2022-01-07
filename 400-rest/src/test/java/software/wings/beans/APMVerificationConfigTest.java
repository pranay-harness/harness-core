/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.SRIRAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.data.structure.CollectionUtils;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

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
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class APMVerificationConfigTest extends WingsBaseTest {
  @Mock SecretManager secretManager;
  @Mock EncryptionService encryptionService;

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testEncryptFields() {
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

    int numOfUrlSecrets = 5;
    int numOfBodySecrets = 7;

    List<String> bodySecrets = new ArrayList<>();
    String validationBody = "{body}";
    for (int i = 0; i < numOfBodySecrets; i++) {
      String secretId = generateUuid();
      validationBody += "&token" + i + "=${secretRef:secretBodyName-" + i + "," + secretId + "}";
      bodySecrets.add(secretId);
    }
    apmVerificationConfig.setValidationBody(validationBody);

    List<String> urlSecrets = new ArrayList<>();
    String validationUrl = "validatiobUrl?";
    for (int i = 0; i < numOfUrlSecrets; i++) {
      String secretId = generateUuid();
      validationUrl += "&token" + i + "=${secretRef:secretUrlName-" + i + "," + secretId + "}";
      urlSecrets.add(secretId);
    }
    apmVerificationConfig.setValidationUrl(validationUrl);

    apmVerificationConfig.encryptFields();

    assertThat(apmVerificationConfig.getHeadersList()).hasSize(2);
    assertThat(apmVerificationConfig.getHeadersList().get(0).getValue()).isEqualTo(headerSecretRef);
    assertThat(apmVerificationConfig.getHeadersList().get(0).getEncryptedValue()).isEqualTo(headerSecretRef);
    assertThat(apmVerificationConfig.getHeadersList().get(1).getValue()).isEqualTo("123");

    assertThat(apmVerificationConfig.getOptionsList()).hasSize(2);
    assertThat(apmVerificationConfig.getOptionsList().get(0).getValue()).isEqualTo(optionSecretRef);
    assertThat(apmVerificationConfig.getOptionsList().get(0).getEncryptedValue()).isEqualTo(optionSecretRef);
    assertThat(apmVerificationConfig.getOptionsList().get(1).getValue()).isEqualTo("321");

    assertThat(apmVerificationConfig.getSecretIdsToFieldNameMap().size())
        .isEqualTo(numOfBodySecrets + numOfUrlSecrets + 2);

    Map<String, String> expectedSecretIdsToFieldNameMap = new HashMap<>();
    for (int i = 0; i < numOfBodySecrets; i++) {
      expectedSecretIdsToFieldNameMap.put(bodySecrets.get(i), "secretBodyName-" + i);
    }
    for (int i = 0; i < numOfUrlSecrets; i++) {
      expectedSecretIdsToFieldNameMap.put(urlSecrets.get(i), "secretUrlName-" + i);
    }
    expectedSecretIdsToFieldNameMap.put(headerSecretRef, "header.api_key");
    expectedSecretIdsToFieldNameMap.put(optionSecretRef, "option.option_key");

    assertThat(apmVerificationConfig.getSecretIdsToFieldNameMap()).isEqualTo(expectedSecretIdsToFieldNameMap);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testFetchRelevantEncryptedSecrets() {
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
    expectedRelevantSecretIds.add("123");
    expectedRelevantSecretIds.add("321");
    expectedRelevantSecretIds.addAll(urlSecrets);
    expectedRelevantSecretIds.addAll(bodySecrets);

    assertThat(CollectionUtils.isEqualCollection(
                   expectedRelevantSecretIds, apmVerificationConfig.fetchRelevantEncryptedSecrets()))
        .isTrue();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testCreateAPMValidateCollectorConfig() throws IOException {
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

    when(secretManager.encryptedDataDetails("111", "api_key", headerSecretRef, null))
        .thenReturn(headerEncryptedDataDetail);
    when(secretManager.encryptedDataDetails("111", "option_key", optionSecretRef, null))
        .thenReturn(optionEncryptedDataDetail);
    when(encryptionService.getDecryptedValue(headerEncryptedDataDetail.get(), false))
        .thenReturn("decryptedHeader".toCharArray());
    when(encryptionService.getDecryptedValue(optionEncryptedDataDetail.get(), false))
        .thenReturn("decryptedOption".toCharArray());
    apmVerificationConfig.setHeadersList(headers);
    apmVerificationConfig.setOptionsList(options);
    apmVerificationConfig.setAccountId("111");
    apmVerificationConfig.setUrl("base");
    apmVerificationConfig.setValidationUrl("suffix");
    APMValidateCollectorConfig apmValidateCollectorConfig =
        apmVerificationConfig.createAPMValidateCollectorConfig(secretManager, encryptionService);
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
