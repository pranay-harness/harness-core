/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.server;

import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConnectorTest extends CategoryTest {
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldFailIfSecureConnectorWithoutKeyFile() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> new Connector(123, true, "", null));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldFailIfSecureConnectorWithoutCertFile() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> new Connector(123, true, null, ""));
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPassIfSecureConnectorWithKeyFileAndCertFile() throws Exception {
    assertThatCode(() -> new Connector(123, true, "", "")).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldPassInsecureConnectorWithoutKeyFileAndCertFile() throws Exception {
    assertThatCode(() -> new Connector(123, false, null, null)).doesNotThrowAnyException();
  }
}
