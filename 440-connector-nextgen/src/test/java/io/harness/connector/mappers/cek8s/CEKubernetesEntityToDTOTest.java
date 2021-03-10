package io.harness.connector.mappers.cek8s;

import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.cek8s.CEK8sDetails;
import io.harness.delegate.beans.connector.cek8s.CEKubernetesClusterConfigDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CEKubernetesEntityToDTOTest extends CategoryTest {
  @InjectMocks CEKubernetesEntityToDTO ceKubernetesEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void testCreateConnectorDTO() {
    final String connectorRef = "connectorRef";
    final CEK8sDetails cek8sDetails = CEK8sDetails.builder().connectorRef(connectorRef).build();
    final CEKubernetesClusterConfigDTO ceKubernetesClusterConfigDTO =
        CEKubernetesClusterConfigDTO.builder().connectorRef(connectorRef).build();

    assertThat(ceKubernetesEntityToDTO.createConnectorDTO(cek8sDetails)).isEqualTo(ceKubernetesClusterConfigDTO);
  }
}