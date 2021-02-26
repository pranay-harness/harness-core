package io.harness.cdng.k8s;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class K8sStepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private ConnectorService connectorService;
  @InjectMocks private K8sStepHelper k8sStepHelper;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetProjectConnector() {
    Ambiance ambiance = getAmbiance();
    ConnectorInfoDTO connectorDTO = ConnectorInfoDTO.builder().build();
    Optional<ConnectorResponseDTO> connectorDTOOptional =
        Optional.of(ConnectorResponseDTO.builder().connector(connectorDTO).build());
    doReturn(connectorDTOOptional).when(connectorService).get("account1", "org1", "project1", "abcConnector");

    ConnectorInfoDTO actualConnector = k8sStepHelper.getConnector("abcConnector", ambiance);
    assertThat(actualConnector).isEqualTo(connectorDTO);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetOrgConnector() {
    Ambiance ambiance = getAmbiance();
    ConnectorInfoDTO connectorDTO = ConnectorInfoDTO.builder().build();
    Optional<ConnectorResponseDTO> connectorDTOOptional =
        Optional.of(ConnectorResponseDTO.builder().connector(connectorDTO).build());
    doReturn(connectorDTOOptional).when(connectorService).get("account1", "org1", null, "abcConnector");

    ConnectorInfoDTO actualConnector = k8sStepHelper.getConnector("org.abcConnector", ambiance);
    assertThat(actualConnector).isEqualTo(connectorDTO);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetAccountConnector() {
    Ambiance ambiance = getAmbiance();
    ConnectorInfoDTO connectorDTO = ConnectorInfoDTO.builder().build();
    Optional<ConnectorResponseDTO> connectorDTOOptional =
        Optional.of(ConnectorResponseDTO.builder().connector(connectorDTO).build());

    doReturn(connectorDTOOptional).when(connectorService).get("account1", null, null, "abcConnector");
    doReturn(Optional.empty()).when(connectorService).get("account1", "org1", null, "abcConnector");
    doReturn(Optional.empty()).when(connectorService).get("account1", "org1", "project1", "abcConnector");

    ConnectorInfoDTO actualConnector = k8sStepHelper.getConnector("account.abcConnector", ambiance);
    assertThat(actualConnector).isEqualTo(connectorDTO);

    assertThatThrownBy(() -> k8sStepHelper.getConnector("org.abcConnector", ambiance))
        .hasMessageContaining("Connector not found for identifier : [org.abcConnector]");

    assertThatThrownBy(() -> k8sStepHelper.getConnector("abcConnector", ambiance))
        .hasMessageContaining("Connector not found for identifier : [abcConnector]");
  }

  public Ambiance getAmbiance() {
    Map<String, String> setupAbstractions = new HashMap<>();

    setupAbstractions.put(SetupAbstractionKeys.accountId, "account1");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "org1");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "project1");

    return Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetK8sManifestsOutcome() {
    assertThatThrownBy(() -> k8sStepHelper.getK8sManifestOutcome(Collections.emptyList()))
        .hasMessageContaining("K8s Manifests are mandatory for k8s Rolling step");

    K8sManifestOutcome k8sManifestOutcome = K8sManifestOutcome.builder().build();
    ValuesManifestOutcome valuesManifestOutcome = ValuesManifestOutcome.builder().build();
    List<ManifestOutcome> serviceManifestOutcomes = new ArrayList<>();
    serviceManifestOutcomes.add(k8sManifestOutcome);
    serviceManifestOutcomes.add(valuesManifestOutcome);

    K8sManifestOutcome actualK8sManifest = k8sStepHelper.getK8sManifestOutcome(serviceManifestOutcomes);
    assertThat(actualK8sManifest).isEqualTo(k8sManifestOutcome);

    K8sManifestOutcome anotherK8sManifest = K8sManifestOutcome.builder().build();
    serviceManifestOutcomes.add(anotherK8sManifest);

    assertThatThrownBy(() -> k8sStepHelper.getK8sManifestOutcome(serviceManifestOutcomes))
        .hasMessageContaining("There can be only a single K8s manifest");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void testGetAggregatedValuesManifests() {
    K8sManifestOutcome k8sManifestOutcome = K8sManifestOutcome.builder().build();
    ValuesManifestOutcome valuesManifestOutcome = ValuesManifestOutcome.builder().build();
    List<ManifestOutcome> serviceManifestOutcomes = new ArrayList<>();
    serviceManifestOutcomes.add(k8sManifestOutcome);
    serviceManifestOutcomes.add(valuesManifestOutcome);

    List<ValuesManifestOutcome> aggregatedValuesManifests =
        k8sStepHelper.getAggregatedValuesManifests(serviceManifestOutcomes);
    assertThat(aggregatedValuesManifests).hasSize(1);
    assertThat(aggregatedValuesManifests.get(0)).isEqualTo(valuesManifestOutcome);
  }
}
