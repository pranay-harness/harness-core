/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.persistence.AccountAccess.ACCOUNT_ID_KEY;
import static io.harness.persistence.UuidAccess.UUID_KEY;
import static io.harness.rule.OwnerRule.ARVIND;

import static software.wings.beans.Service.APP_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.service.intfc.AppService;
import software.wings.service.intfc.security.SecretManager;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@OwnedBy(HarnessTeam.CDP)
public class InfraProvisionStepYamlBuilderTest extends CategoryTest {
  public static final String SECRET_NAME_ERROR_MESSAGE = "Could not find secret value for name: SECRET_NAME";
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private AppService appService;
  @Mock private SecretManager secretManager;
  @InjectMocks private InfraProvisionStepYamlBuilder infraProvisionStepYamlBuilder;

  private static final String PROPERTY_NAME = "PROPERTY_NAME";

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testConvertPropertyIdsToNames() {
    infraProvisionStepYamlBuilder.convertPropertyIdsToNames(PROPERTY_NAME, APP_ID, null);
    infraProvisionStepYamlBuilder.convertPropertyIdsToNames(PROPERTY_NAME, null, null);
    doReturn(null).when(appService).getAccountIdByAppId(APP_ID);
    infraProvisionStepYamlBuilder.convertPropertyIdsToNames(PROPERTY_NAME, APP_ID, null);
    doReturn(ACCOUNT_ID_KEY).when(appService).getAccountIdByAppId(APP_ID);
    BasicDBList subProperties = new BasicDBList();
    BasicDBObject e = getVariableObject();
    subProperties.add(e);

    infraProvisionStepYamlBuilder.convertPropertyIdsToNames(PROPERTY_NAME, APP_ID, subProperties);
    assertThat(((BasicDBObject) subProperties.get(0)).get("value")).isEqualTo("safeharness:ABC");
    e.put("value", UUID_KEY);
    doReturn("secret_ref_with_name").when(secretManager).getEncryptedYamlRef(ACCOUNT_ID_KEY, UUID_KEY);
    infraProvisionStepYamlBuilder.convertPropertyIdsToNames(PROPERTY_NAME, APP_ID, subProperties);
    assertThat(((BasicDBObject) subProperties.get(0)).get("value")).isEqualTo("secret_ref_with_name");

    e.put("value", null);
    infraProvisionStepYamlBuilder.convertPropertyIdsToNames(PROPERTY_NAME, APP_ID, subProperties);
    e.put("value", "");
    infraProvisionStepYamlBuilder.convertPropertyIdsToNames(PROPERTY_NAME, APP_ID, subProperties);
    e.put("value", "safeharness1:ABC");
    infraProvisionStepYamlBuilder.convertPropertyIdsToNames(PROPERTY_NAME, APP_ID, subProperties);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testConvertPropertyNamesToIds() {
    infraProvisionStepYamlBuilder.convertPropertyNamesToIds(PROPERTY_NAME, ACCOUNT_ID_KEY, null);
    infraProvisionStepYamlBuilder.convertPropertyNamesToIds(PROPERTY_NAME, null, null);

    BasicDBList subProperties = new BasicDBList();
    BasicDBObject e = getVariableObject();
    subProperties.add(e);

    doReturn(EncryptedData.builder().uuid(UUID_KEY).build())
        .when(secretManager)
        .getEncryptedDataFromYamlRef("safeharness:ABC", ACCOUNT_ID_KEY);
    infraProvisionStepYamlBuilder.convertPropertyNamesToIds(PROPERTY_NAME, ACCOUNT_ID_KEY, subProperties);
    assertThat(((BasicDBObject) subProperties.get(0)).get("value")).isEqualTo(UUID_KEY);

    e.put("value", UUID_KEY);
    doReturn(null).when(secretManager).getEncryptedYamlRef(ACCOUNT_ID_KEY, UUID_KEY);
    infraProvisionStepYamlBuilder.convertPropertyNamesToIds(PROPERTY_NAME, ACCOUNT_ID_KEY, subProperties);
    assertThat(((BasicDBObject) subProperties.get(0)).get("value")).isEqualTo(UUID_KEY);

    e.put("value", null);
    assertThatThrownBy(
        () -> infraProvisionStepYamlBuilder.convertPropertyNamesToIds(PROPERTY_NAME, ACCOUNT_ID_KEY, subProperties))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(SECRET_NAME_ERROR_MESSAGE);
    e.put("value", "");
    assertThatThrownBy(
        () -> infraProvisionStepYamlBuilder.convertPropertyNamesToIds(PROPERTY_NAME, ACCOUNT_ID_KEY, subProperties))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage(SECRET_NAME_ERROR_MESSAGE);

    e.put("value", "safeharness1:ABC");
    assertThatThrownBy(
        () -> infraProvisionStepYamlBuilder.convertPropertyNamesToIds(PROPERTY_NAME, ACCOUNT_ID_KEY, subProperties))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Could not find type: safeharness1 in account: " + ACCOUNT_ID_KEY);
  }

  @NotNull
  private BasicDBObject getVariableObject() {
    BasicDBObject e = new BasicDBObject("valueType", "ENCRYPTED_TEXT");
    e.put("name", "SECRET_NAME");
    e.put("value", "safeharness:ABC");
    return e;
  }
}
