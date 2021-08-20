package io.harness.connector.mappers.argo;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.argo.ArgoConnector;
import io.harness.delegate.beans.connector.argo.ArgoConnectorDTO;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ArgoEntityToDTOTest extends CategoryTest {
  private ArgoEntityToDTO mapper;

  @Before
  public void setUp() {
    mapper = new ArgoEntityToDTO();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testEntityToDTO() {
    final String adapterUrl = "https://1.2.3.4";
    ArgoConnector entity = ArgoConnector.builder().adapterUrl(adapterUrl).build();

    ArgoConnectorDTO dto = mapper.createConnectorDTO(entity);
    assertThat(dto.getAdapterUrl()).isEqualTo(adapterUrl);
  }
}