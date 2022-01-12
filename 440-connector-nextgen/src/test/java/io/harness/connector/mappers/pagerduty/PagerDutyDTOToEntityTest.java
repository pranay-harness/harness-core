/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.encryption.SecretRefData.SECRET_DOT_DELIMINITER;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.pagerduty.PagerDutyConnector;
import io.harness.delegate.beans.connector.pagerduty.PagerDutyConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CV)
public class PagerDutyDTOToEntityTest extends CategoryTest {
  @InjectMocks PagerDutyDTOToEntity pagerDutyDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testToPagerDutyConnector() {
    String encryptedApiToken = "encryptedApiToken";
    SecretRefData secretRefData = SecretRefData.builder().identifier(encryptedApiToken).scope(ACCOUNT).build();
    PagerDutyConnectorDTO pagerDutyConnectorDTO = PagerDutyConnectorDTO.builder().apiTokenRef(secretRefData).build();

    PagerDutyConnector pagerDutyConnector = pagerDutyDTOToEntity.toConnectorEntity(pagerDutyConnectorDTO);
    assertThat(pagerDutyConnector).isNotNull();
    assertThat(pagerDutyConnector.getApiTokenRef()).isNotNull();
    assertThat(pagerDutyConnector.getApiTokenRef())
        .isEqualTo(ACCOUNT.getYamlRepresentation() + SECRET_DOT_DELIMINITER
            + pagerDutyConnectorDTO.getApiTokenRef().getIdentifier());
  }
}
