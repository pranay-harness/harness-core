/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.sumologic;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.sumologic.SumoLogicConnector;
import io.harness.connector.mappers.sumologicmapper.SumoLogicDTOToEntity;
import io.harness.delegate.beans.connector.sumologic.SumoLogicConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CV)
public class SumoLogicDTOToEntityTest extends CategoryTest {
  @InjectMocks private SumoLogicDTOToEntity sumoLogicDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testConvertEntitiyToDto() {
    String url = "https://api.in.sumologic.com/api/";
    SecretRefData accessIdSecretRefData = SecretRefData.builder().identifier("accessId").scope(ACCOUNT).build();
    SecretRefData accessKeySecretRefData = SecretRefData.builder().identifier("accessJey").scope(ACCOUNT).build();
    SumoLogicConnectorDTO connectorDTO = SumoLogicConnectorDTO.builder()
                                             .accessIdRef(accessIdSecretRefData)
                                             .accessKeyRef(accessKeySecretRefData)
                                             .url(url)
                                             .build();

    SumoLogicConnector connector = sumoLogicDTOToEntity.toConnectorEntity(connectorDTO);
    assertThat(connector).isNotNull();
    assertThat(connector.getUrl()).isEqualTo(connectorDTO.getUrl());
    assertThat(connector.getAccessIdRef())
        .isEqualTo(ACCOUNT.getYamlRepresentation() + "." + connectorDTO.getAccessIdRef().getIdentifier());
    assertThat(connector.getAccessKeyRef())
        .isEqualTo(ACCOUNT.getYamlRepresentation() + "." + connectorDTO.getAccessKeyRef().getIdentifier());
  }
}
