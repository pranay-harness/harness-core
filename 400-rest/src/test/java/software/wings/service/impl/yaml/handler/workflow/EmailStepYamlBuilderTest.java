/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.MILOS;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.IncompleteStateException;
import software.wings.utils.WingsTestConstants;
import software.wings.yaml.workflow.StepYaml;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@OwnedBy(HarnessTeam.CDC)
public class EmailStepYamlBuilderTest extends WingsBaseTest {
  private static final String TO_ADDRESS = "toAddress";
  private static final String CC_ADDRESS = "ccAddress";
  private static final String SUBJECT = "subject";
  private static final String BODY = "body";
  private static final String IGNORE_DELIVERY_FAILURE = "ignoreDeliveryFailure";

  @InjectMocks @Inject private EmailStepYamlBuilder validator;

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testValidateEmailStepYaml() {
    Map<String, Object> properties = getProperties(TO_ADDRESS, CC_ADDRESS, SUBJECT, BODY, false);
    validator.validate(buildChangeContext(properties));
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testValidateEmailStepYamlWithoutToAddress() {
    Map<String, Object> properties = getProperties(null, CC_ADDRESS, SUBJECT, BODY, false);
    ChangeContext changeContext = buildChangeContext(properties);

    assertThatThrownBy(() -> validator.validate(changeContext)).isInstanceOf(IncompleteStateException.class);
    assertThatThrownBy(() -> validator.validate(changeContext))
        .hasMessage("\"toAddress\" could not be empty or null, please provide a value.");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testValidateEmailStepYamlWithEmptyToAddress() {
    Map<String, Object> properties = getProperties(" ", CC_ADDRESS, SUBJECT, BODY, false);
    ChangeContext changeContext = buildChangeContext(properties);

    assertThatThrownBy(() -> validator.validate(changeContext)).isInstanceOf(IncompleteStateException.class);
    assertThatThrownBy(() -> validator.validate(changeContext))
        .hasMessage("\"toAddress\" could not be empty or null, please provide a value.");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testValidateEmailStepYamlWithoutSubject() {
    Map<String, Object> properties = getProperties(TO_ADDRESS, CC_ADDRESS, null, BODY, false);
    ChangeContext changeContext = buildChangeContext(properties);

    assertThatThrownBy(() -> validator.validate(changeContext)).isInstanceOf(IncompleteStateException.class);
    assertThatThrownBy(() -> validator.validate(changeContext))
        .hasMessage("\"subject\" could not be empty or null, please provide a value.");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testValidateEmailStepYamlWithEmptySubject() {
    Map<String, Object> properties = getProperties(TO_ADDRESS, CC_ADDRESS, " ", BODY, false);
    ChangeContext changeContext = buildChangeContext(properties);

    assertThatThrownBy(() -> validator.validate(changeContext)).isInstanceOf(IncompleteStateException.class);
    assertThatThrownBy(() -> validator.validate(changeContext))
        .hasMessage("\"subject\" could not be empty or null, please provide a value.");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testValidateEmailStepYamlWithoutBody() {
    Map<String, Object> properties = getProperties(TO_ADDRESS, CC_ADDRESS, SUBJECT, null, false);
    ChangeContext changeContext = buildChangeContext(properties);

    assertThatThrownBy(() -> validator.validate(changeContext)).isInstanceOf(IncompleteStateException.class);
    assertThatThrownBy(() -> validator.validate(changeContext))
        .hasMessage("\"body\" could not be empty or null, please provide a value.");
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testValidateEmailStepYamlWithEmptyBody() {
    Map<String, Object> properties = getProperties(TO_ADDRESS, CC_ADDRESS, SUBJECT, " ", false);
    ChangeContext changeContext = buildChangeContext(properties);

    assertThatThrownBy(() -> validator.validate(changeContext)).isInstanceOf(IncompleteStateException.class);
    assertThatThrownBy(() -> validator.validate(changeContext))
        .hasMessage("\"body\" could not be empty or null, please provide a value.");
  }

  private ChangeContext buildChangeContext(Map<String, Object> parameters) {
    return ChangeContext.Builder.aChangeContext()
        .withYaml(StepYaml.builder().properties(parameters).build())
        .withChange(Change.Builder.aFileChange().withAccountId(WingsTestConstants.ACCOUNT_ID).build())
        .build();
  }

  private Map<String, Object> getProperties(
      String toAddress, String ccAddress, String subject, String body, boolean ignore) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(TO_ADDRESS, toAddress);
    properties.put(CC_ADDRESS, ccAddress);
    properties.put(SUBJECT, subject);
    properties.put(BODY, body);
    properties.put(IGNORE_DELIVERY_FAILURE, ignore);

    return properties;
  }
}
