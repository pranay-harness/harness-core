/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.stackdriver;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StackdriverUtilsTest extends CategoryTest {
  private List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

  @Before
  public void setupTests() {
    initMocks(this);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilitiesForMetrics() {
    List<ExecutionCapability> executionCapabilities =
        StackdriverUtils.fetchRequiredExecutionCapabilitiesForMetrics(encryptedDataDetails, null);
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        (HttpConnectionExecutionCapability) executionCapabilities.get(0);
    assertThat(httpConnectionExecutionCapability.fetchCapabilityBasis())
        .isEqualTo("https://monitoring.googleapis.com/$discovery/rest?version=v1");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilitiesForLogs() {
    List<ExecutionCapability> executionCapabilities =
        StackdriverUtils.fetchRequiredExecutionCapabilitiesForLogs(encryptedDataDetails, null);
    assertThat(executionCapabilities).hasSize(1);
    assertThat(executionCapabilities.get(0)).isInstanceOf(HttpConnectionExecutionCapability.class);
    HttpConnectionExecutionCapability httpConnectionExecutionCapability =
        (HttpConnectionExecutionCapability) executionCapabilities.get(0);
    assertThat(httpConnectionExecutionCapability.fetchCapabilityBasis())
        .isEqualTo("https://logging.googleapis.com/$discovery/rest?version=v2");
  }
}
