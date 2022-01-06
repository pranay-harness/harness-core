/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.verification;

import static io.harness.rule.OwnerRule.SOWMYA;

import static software.wings.verification.prometheus.PrometheusCVServiceConfiguration.PrometheusCVConfigurationYaml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class CVConfigurationYamlTest extends WingsBaseTest {
  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetEnabled24x7() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    yaml.setEnabled24x7(true);
    assertThat(yaml.isEnabled24x7()).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetEnabled24x7WithInteger() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    assertThatThrownBy(() -> yaml.setEnabled24x7(12)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetEnabled24x7WithRandomString() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    assertThatThrownBy(() -> yaml.setEnabled24x7("Random")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetAlertEnabled_whenSet() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    yaml.setAlertEnabled(true);
    assertThat(yaml.isAlertEnabled()).isTrue();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetAlertEnabled_whenNotSet() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    assertThat(yaml.isAlertEnabled()).isFalse();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetAlertEnabled_withInteger() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    assertThatThrownBy(() -> yaml.setAlertEnabled(12)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSetAlertEnabled_withRandomString() {
    CVConfigurationYaml yaml = new PrometheusCVConfigurationYaml(new ArrayList<>());
    assertThatThrownBy(() -> yaml.setAlertEnabled("Random")).isInstanceOf(IllegalArgumentException.class);
  }
}
