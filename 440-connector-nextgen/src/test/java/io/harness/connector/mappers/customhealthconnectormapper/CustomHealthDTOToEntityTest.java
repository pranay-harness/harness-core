/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.customhealthconnectormapper;

import static io.harness.rule.OwnerRule.ANJAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.customhealthconnector.CustomHealthConnector;
import io.harness.connector.entities.embedded.customhealthconnector.CustomHealthConnectorKeyAndValue;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthConnectorDTO;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthKeyAndValue;
import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CustomHealthDTOToEntityTest extends CategoryTest {
  static final String baseURLWithoutSlash = "http://dyna.com";
  static final String baseURLlWithSlash = baseURLWithoutSlash + "/";
  List<CustomHealthKeyAndValue> dtoParams = Arrays.asList(new CustomHealthKeyAndValue[] {
      CustomHealthKeyAndValue.builder().key("identifier").value("sdfsdf").isValueEncrypted(false).build()});
  List<CustomHealthKeyAndValue> dtoHeaders =
      Arrays.asList(new CustomHealthKeyAndValue[] {CustomHealthKeyAndValue.builder()
                                                       .key("api_key")
                                                       .encryptedValueRef(SecretRefHelper.createSecretRef("21312sdfs"))
                                                       .isValueEncrypted(true)
                                                       .build(),
          CustomHealthKeyAndValue.builder()
              .key("api_key")
              .encryptedValueRef(SecretRefHelper.createSecretRef("21312sdfs"))
              .isValueEncrypted(true)
              .build()});
  String validationPath = "/sfsdf?ssdf=232";
  CustomHealthMethod customHealthMethod = CustomHealthMethod.GET;

  List<CustomHealthConnectorKeyAndValue> connectorParams = Arrays.asList(new CustomHealthConnectorKeyAndValue[] {
      CustomHealthConnectorKeyAndValue.builder().key("identifier").value("sdfsdf").isValueEncrypted(false).build()});
  List<CustomHealthConnectorKeyAndValue> connectorHeaders =
      Arrays.asList(new CustomHealthConnectorKeyAndValue[] {CustomHealthConnectorKeyAndValue.builder()
                                                                .key("api_key")
                                                                .encryptedValueRef("21312sdfs")
                                                                .isValueEncrypted(true)
                                                                .build(),
          CustomHealthConnectorKeyAndValue.builder()
              .key("api_key")
              .encryptedValueRef("21312sdfs")
              .isValueEncrypted(true)
              .build()});

  @InjectMocks CustomHealthDTOToEntity customHealthDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testCustomHealthDTOToEntityConnector_withoutURLSlash() {
    CustomHealthConnectorDTO connectorDTO = CustomHealthConnectorDTO.builder()
                                                .baseURL(baseURLWithoutSlash)
                                                .delegateSelectors(Collections.emptySet())
                                                .validationPath(validationPath)
                                                .headers(dtoHeaders)
                                                .params(dtoParams)
                                                .method(customHealthMethod)
                                                .build();

    CustomHealthConnector customHealthConnector = customHealthDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(customHealthConnector).isNotNull();
    assertThat(customHealthConnector.getBaseURL()).isEqualTo(baseURLlWithSlash);
    assertThat(customHealthConnector.getValidationPath()).isEqualTo(validationPath);
    assertThat(customHealthConnector.getValidationBody()).isNull();
    assertThat(customHealthConnector.getMethod()).isEqualTo(CustomHealthMethod.GET);
    assertThat(customHealthConnector.getParams()).isEqualTo(connectorParams);
    assertThat(customHealthConnector.getHeaders()).isEqualTo(connectorHeaders);
  }

  @Test
  @Owner(developers = ANJAN)
  @Category(UnitTests.class)
  public void testCustomHealthDTOToEntityConnector_withURLSlash() {
    CustomHealthConnectorDTO connectorDTO = CustomHealthConnectorDTO.builder()
                                                .baseURL(baseURLlWithSlash)
                                                .delegateSelectors(Collections.emptySet())
                                                .validationPath(validationPath)
                                                .headers(dtoHeaders)
                                                .params(dtoParams)
                                                .method(customHealthMethod)
                                                .build();

    CustomHealthConnector customHealthConnector = customHealthDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(customHealthConnector).isNotNull();
    assertThat(customHealthConnector.getBaseURL()).isEqualTo(baseURLlWithSlash);
    assertThat(customHealthConnector.getValidationPath()).isEqualTo(validationPath);
    assertThat(customHealthConnector.getValidationBody()).isNull();
    assertThat(customHealthConnector.getMethod()).isEqualTo(CustomHealthMethod.GET);
    System.out.println("sdfsf");
    assertThat(customHealthConnector.getParams()).isEqualTo(connectorParams);
    assertThat(customHealthConnector.getHeaders()).isEqualTo(connectorHeaders);
  }
}
