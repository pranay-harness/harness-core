/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.sumologic;

import static io.harness.annotations.dev.HarnessTeam.CV;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.sumologic.SumoLogicConnector;
import io.harness.connector.mappers.sumologicmapper.SumoLogicEntityToDTO;
import io.harness.delegate.beans.connector.sumologic.SumoLogicConnectorDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(CV)
public class SumoLogicEntityToDTOTest extends CategoryTest {
  @InjectMocks private SumoLogicEntityToDTO sumoLogicEntityToDTO;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testConvertEntitiyToDto() {
    String apiKeyRef = "accessIdRef";
    String applicationKeyRef = "accessKeyRef";
    String url = "https://api.in.sumologic.com/api/";

    SumoLogicConnector connector =
        SumoLogicConnector.builder().url(url).accessIdRef(apiKeyRef).accessKeyRef(applicationKeyRef).build();

    SumoLogicConnectorDTO connectorDTO = sumoLogicEntityToDTO.createConnectorDTO(connector);
    assertThat(connectorDTO).isNotNull();
    assertThat(connector.getUrl()).isEqualTo(connectorDTO.getUrl());
    assertThat(connectorDTO.getAccessIdRef().getIdentifier()).isEqualTo(connector.getAccessIdRef());
    assertThat(connectorDTO.getAccessKeyRef().getIdentifier()).isEqualTo(connector.getAccessKeyRef());
  }
}
