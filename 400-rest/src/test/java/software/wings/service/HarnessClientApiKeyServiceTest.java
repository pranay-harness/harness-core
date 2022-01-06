/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.rule.OwnerRule.RAMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ClientType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.HarnessApiKeyService;

import com.google.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;

/**
 *
 * @author rktummala
 */
public class HarnessClientApiKeyServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private HarnessApiKeyService harnessApiKeyService;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testCRUD() {
    String generatedApiKey = harnessApiKeyService.generate(ClientType.PROMETHEUS.name());
    assertThat(generatedApiKey).isNotNull();

    String keyFromGet = harnessApiKeyService.get(ClientType.PROMETHEUS.name());
    assertThat(generatedApiKey).isEqualTo(keyFromGet);

    String salesForceKey = harnessApiKeyService.generate(ClientType.SALESFORCE.name());
    assertThat(generatedApiKey).isNotEqualTo(salesForceKey);

    thrown.expect(Exception.class);
    harnessApiKeyService.generate(ClientType.PROMETHEUS.name());

    harnessApiKeyService.delete(ClientType.PROMETHEUS.name());

    thrown.expect(Exception.class);
    harnessApiKeyService.get(ClientType.PROMETHEUS.name());
  }
}
