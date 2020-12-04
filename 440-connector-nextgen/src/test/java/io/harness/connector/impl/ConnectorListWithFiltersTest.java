package io.harness.connector.impl;

import static io.harness.connector.entities.ConnectivityStatus.FAILURE;
import static io.harness.connector.entities.ConnectivityStatus.SUCCESS;
import static io.harness.connector.impl.ConnectorFilterServiceImpl.CREDENTIAL_TYPE_KEY;
import static io.harness.connector.impl.ConnectorFilterServiceImpl.INHERIT_FROM_DELEGATE_STRING;
import static io.harness.delegate.beans.connector.ConnectorCategory.ARTIFACTORY;
import static io.harness.delegate.beans.connector.ConnectorType.AWS;
import static io.harness.delegate.beans.connector.ConnectorType.DOCKER;
import static io.harness.delegate.beans.connector.ConnectorType.GCP;
import static io.harness.delegate.beans.connector.ConnectorType.KUBERNETES_CLUSTER;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.INHERIT_FROM_DELEGATE;
import static io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType.MANUAL_CREDENTIALS;
import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.encryption.SecretRefData.SECRET_DOT_DELIMINITER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorsTestBase;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorFilterDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorListFilter;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.entities.ConnectivityStatus;
import io.harness.connector.entities.ConnectorConnectivityDetails;
import io.harness.connector.entities.embedded.awsconnector.AwsConfig.AwsConfigKeys;
import io.harness.connector.entities.embedded.gcpconnector.GcpConfig.GcpConfigKeys;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig.KubernetesClusterConfigKeys;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.connector.awsconnector.AwsInheritFromDelegateSpecDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpDelegateDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesDelegateDetailsDTO;
import io.harness.encryption.Scope;
import io.harness.repositories.ConnectorRepository;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.data.domain.Page;

@Slf4j
public class ConnectorListWithFiltersTest extends ConnectorsTestBase {
  @Inject DefaultConnectorServiceImpl connectorService;
  @Inject ConnectorRepository connectorRepository;
  @Inject ConnectorFilterService connectorFilterService;
  String accountIdentifier = "accountIdentifier";
  String orgIdentifier = "orgIdentifier";
  String projectIdentifier = "projectIdentifier";
  String name = "name";
  String identifier = "identifier";
  String description = "description";
  ConnectorType connectorType = ConnectorType.DOCKER;

  private ConnectorInfoDTO getConnector(String name, String identifier, String description) {
    return ConnectorInfoDTO.builder()
        .name(name)
        .identifier(identifier)
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .description(description)
        .connectorType(connectorType)
        .connectorConfig(DockerConnectorDTO.builder().build())
        .build();
  }

  private void createConnectorsWithGivenOrgs(List<String> orgs) {
    ConnectorInfoDTO connectorInfoDTO = getConnector(name, identifier, description);
    for (String orgId : orgs) {
      connectorInfoDTO.setOrgIdentifier(orgId);
      connectorInfoDTO.setProjectIdentifier(null);
      connectorService.create(ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), accountIdentifier);
    }
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListWithOrgFilters() {
    createConnectorsWithGivenOrgs(Arrays.asList("org1", "org2", "org3", "org4", "org1", "org5"));
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(0, 100, accountIdentifier,
        ConnectorListFilter.builder()
            .orgIdentifier(Arrays.asList("org1", "org4"))
            .scope(Collections.singletonList(Scope.ORG))
            .build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(3);
    List<ConnectorResponseDTO> connectorResponseDTOS = connectorDTOS.getContent();
    List<String> orgIdsOfConnectors =
        connectorResponseDTOS.stream()
            .map(connectorResponseDTO -> connectorResponseDTO.getConnector().getOrgIdentifier())
            .collect(Collectors.toList());
    assertThat(orgIdsOfConnectors.containsAll(Arrays.asList("org1", "org4", "org1"))).isTrue();
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListWithProjectFilters() {
    createConnectorsWithGivenProjects(Arrays.asList("proj1", "proj1", "proj1", "proj2", "proj3", "proj4"));
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(0, 100, accountIdentifier,
        ConnectorListFilter.builder()
            .projectIdentifier(Arrays.asList("proj1", "proj2"))
            .scope(Collections.singletonList(Scope.PROJECT))
            .build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(4);
    List<ConnectorResponseDTO> connectorResponseDTOS = connectorDTOS.getContent();
    List<String> projectIdsOfConnectors =
        connectorResponseDTOS.stream()
            .map(connectorResponseDTO -> connectorResponseDTO.getConnector().getProjectIdentifier())
            .collect(Collectors.toList());
    assertThat(projectIdsOfConnectors.containsAll(Arrays.asList("proj1", "proj1", "proj1", "proj2"))).isTrue();
  }

  private void createConnectorsWithGivenProjects(List<String> projIds) {
    ConnectorInfoDTO connectorInfoDTO = getConnector(name, identifier, description);
    for (String projId : projIds) {
      connectorInfoDTO.setProjectIdentifier(projId);
      connectorService.create(ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), accountIdentifier);
    }
  }

  private void createAccountLevelConnectors() {
    ConnectorInfoDTO connectorInfoDTO = getConnector(name, identifier, description);
    connectorInfoDTO.setOrgIdentifier(null);
    connectorInfoDTO.setProjectIdentifier(null);
    connectorService.create(ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), accountIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListWithAccountScope() {
    createAccountLevelConnectors();
    createConnectorsWithGivenOrgs(Arrays.asList("org1", "org2"));
    createConnectorsWithGivenProjects(Arrays.asList("proj1", "proj2"));
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(0, 100, accountIdentifier,
        ConnectorListFilter.builder().scope(Collections.singletonList(Scope.ACCOUNT)).build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(1);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListForAllConnectorsInAccount() {
    createAccountLevelConnectors();
    createAccountLevelConnectors();
    createConnectorsWithGivenOrgs(Arrays.asList("org1", "org2"));
    createConnectorsWithGivenProjects(Arrays.asList("proj1", "proj2"));
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(0, 100, accountIdentifier,
        ConnectorListFilter.builder().scope(Arrays.asList(Scope.ACCOUNT, Scope.ORG, Scope.PROJECT)).build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(6);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListForAllConnectorsInOrg() {
    createConnectorsWithGivenOrgs(Arrays.asList(orgIdentifier, orgIdentifier));
    createConnectorsWithGivenProjects(Arrays.asList("proj1", "proj2"));
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(0, 100, accountIdentifier,
        ConnectorListFilter.builder()
            .orgIdentifier(Collections.singletonList(orgIdentifier))
            .scope(Arrays.asList(Scope.ORG, Scope.PROJECT))
            .build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(4);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListWithNamesFilter() {
    createConnectorsWithNames(Arrays.asList("docker", "docker connector", "qa connector", "docker"));
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(
        0, 100, accountIdentifier, ConnectorListFilter.builder().name(Arrays.asList("docker")).build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(2);
    List<String> connectorNames = connectorDTOS.stream()
                                      .map(connectorResponseDTO -> connectorResponseDTO.getConnector().getName())
                                      .collect(Collectors.toList());
    assertThat(connectorNames.containsAll(Arrays.asList("docker", "docker"))).isTrue();
  }

  private void createConnectorsWithNames(List<String> namesList) {
    ConnectorInfoDTO connectorInfoDTO = getConnector(name, identifier, description);
    for (String name : namesList) {
      connectorInfoDTO.setName(name);
      connectorService.create(ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), accountIdentifier);
    }
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListWithIdentifierFilter() {
    createConnectorsWithIdentifiers(Arrays.asList("dockerId", "identifier", "qa connector", "dockerId"));
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(0, 100, accountIdentifier,
        ConnectorListFilter.builder().connectorIdentifier(Arrays.asList("dockerId")).build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(2);
    List<String> identifiersName = connectorDTOS.stream()
                                       .map(connectorResponseDTO -> connectorResponseDTO.getConnector().getIdentifier())
                                       .collect(Collectors.toList());
    assertThat(identifiersName.containsAll(Arrays.asList("dockerId", "dockerId"))).isTrue();
  }

  private void createConnectorsWithIdentifiers(List<String> identifiers) {
    ConnectorInfoDTO connectorInfoDTO = getConnector(name, identifier, description);
    for (String identifier : identifiers) {
      connectorInfoDTO.setIdentifier(identifier);
      connectorService.create(ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), accountIdentifier);
    }
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListWithDescriptionFilter() {
    createConnectorsWithDescriptions(
        Arrays.asList("docker connector", "docker", "qa connector", "qa", "harness test", "description"));
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(
        0, 100, accountIdentifier, ConnectorListFilter.builder().description(Arrays.asList("docker", "qa")).build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(4);
    List<String> descriptions = connectorDTOS.stream()
                                    .map(connectorResponseDTO -> connectorResponseDTO.getConnector().getDescription())
                                    .collect(Collectors.toList());
    assertThat(descriptions.containsAll(Arrays.asList("docker connector", "docker", "qa connector", "qa"))).isTrue();
  }

  private void createConnectorsWithDescriptions(List<String> descriptions) {
    ConnectorInfoDTO connectorInfoDTO = getConnector(name, identifier, description);
    for (String description : descriptions) {
      connectorInfoDTO.setDescription(description);
      connectorService.create(ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), accountIdentifier);
    }
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListWithSearchTerm() {
    createConnectorsWithNames(Arrays.asList("docker connector", "docker dev", "qa connector test", "qb"));
    createConnectorsWithIdentifiers(Arrays.asList("docker", "identifier", "dockerId"));
    createConnectorsWithDescriptions(
        Arrays.asList("docker connector description", "docker", "qa connector test", "harness test", "description"));
    Page<ConnectorResponseDTO> connectorDTOS =
        connectorService.list(0, 100, accountIdentifier, ConnectorListFilter.builder().searchTerm("docker").build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(6);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListWithTypes() {
    createConnectorsWithNames(Arrays.asList("docker connector test", "docker dev connector"));
    createK8sConnector();
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(0, 100, accountIdentifier,
        ConnectorListFilter.builder().type(Arrays.asList(KUBERNETES_CLUSTER, ConnectorType.DOCKER)).build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(3);
    List<ConnectorType> connectorTypes =
        connectorDTOS.stream()
            .map(connectorResponseDTO -> connectorResponseDTO.getConnector().getConnectorType())
            .collect(Collectors.toList());
    assertThat(connectorTypes.containsAll(Arrays.asList(KUBERNETES_CLUSTER, DOCKER))).isTrue();
  }

  private void createK8sConnector() {
    ConnectorDTO connectorDTO =
        ConnectorDTO.builder()
            .connectorInfo(
                ConnectorInfoDTO.builder()
                    .orgIdentifier(orgIdentifier)
                    .projectIdentifier(projectIdentifier)
                    .identifier(identifier)
                    .connectorType(KUBERNETES_CLUSTER)
                    .connectorConfig(
                        KubernetesClusterConfigDTO.builder()
                            .credential(KubernetesCredentialDTO.builder()
                                            .kubernetesCredentialType(KubernetesCredentialType.INHERIT_FROM_DELEGATE)
                                            .config(KubernetesDelegateDetailsDTO.builder().build())
                                            .build())
                            .build())
                    .build())
            .build();
    connectorService.create(connectorDTO, accountIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListWithCategories() {
    createConnectorsWithNames(Arrays.asList("docker connector", "docker dev"));
    createK8sConnector();
    Page<ConnectorResponseDTO> connectorDTOS = connectorService.list(0, 100, accountIdentifier,
        ConnectorListFilter.builder().category(Arrays.asList(ConnectorCategory.CLOUD_PROVIDER, ARTIFACTORY)).build());
    assertThat(connectorDTOS).isNotNull();
    assertThat(connectorDTOS.getTotalElements()).isEqualTo(3);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListConnectivityStatus() {
    createConnectorsWithStatus(4, SUCCESS);
    createConnectorsWithStatus(6, FAILURE);
    Page<ConnectorResponseDTO> connectorWithSuccessStatus = connectorService.list(
        0, 100, accountIdentifier, ConnectorListFilter.builder().connectivityStatus(Arrays.asList(SUCCESS)).build());
    assertThat(connectorWithSuccessStatus.getTotalElements()).isEqualTo(4);
    Page<ConnectorResponseDTO> connectorWithFailedStatus = connectorService.list(
        0, 100, accountIdentifier, ConnectorListFilter.builder().connectivityStatus(Arrays.asList(FAILURE)).build());
    assertThat(connectorWithFailedStatus.getTotalElements()).isEqualTo(6);
    Page<ConnectorResponseDTO> allConnectors = connectorService.list(0, 100, accountIdentifier,
        ConnectorListFilter.builder().connectivityStatus(Arrays.asList(SUCCESS, FAILURE)).build());
    assertThat(allConnectors.getTotalElements()).isEqualTo(10);
  }

  private KubernetesClusterConfig getConnectorEntity() {
    String masterURL = "masterURL";
    String userName = "userName";
    String passwordIdentifier = "passwordIdentifier";
    String passwordRef = ACCOUNT + SECRET_DOT_DELIMINITER + passwordIdentifier;
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).passwordRef(passwordRef).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    KubernetesClusterConfig connector = KubernetesClusterConfig.builder()
                                            .credentialType(MANUAL_CREDENTIALS)
                                            .credential(kubernetesClusterDetails)
                                            .build();
    connector.setAccountIdentifier(accountIdentifier);
    connector.setType(KUBERNETES_CLUSTER);
    return connector;
  }

  private void createConnectorsWithStatus(int numberOfConnectors, ConnectivityStatus status) {
    for (int i = 0; i < numberOfConnectors; i++) {
      KubernetesClusterConfig connector = getConnectorEntity();
      connector.setConnectivityDetails(ConnectorConnectivityDetails.builder().status(status).build());
      connectorRepository.save(connector);
    }
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListForInheritFromDelegate() {
    createConnectorsWithNames(Arrays.asList("docker hub", "docker test"));
    createK8sConnectorWithInheritFromDelegate();
    createAWSConnectorWithInheritFromDelegate();
    createGCPConnectorWithInheritFromDelegate();
    Page<ConnectorResponseDTO> connectorsInheritingCredentialsFromDelegate = connectorService.list(
        0, 100, accountIdentifier, ConnectorListFilter.builder().inheritingCredentialsFromDelegate(true).build());
    assertThat(connectorsInheritingCredentialsFromDelegate.getTotalElements()).isEqualTo(3);
    Page<ConnectorResponseDTO> connectorsNotInheritingCredentialsFromDelegate = connectorService.list(
        0, 100, accountIdentifier, ConnectorListFilter.builder().inheritingCredentialsFromDelegate(false).build());
    assertThat(connectorsNotInheritingCredentialsFromDelegate.getTotalElements()).isEqualTo(2);
    Page<ConnectorResponseDTO> connectorsWhenFilterIsNotGiven = connectorService.list(0, 100, accountIdentifier, null);
    assertThat(connectorsWhenFilterIsNotGiven.getTotalElements()).isEqualTo(5);
  }

  private void createGCPConnectorWithInheritFromDelegate() {
    String delegateSelector = "delegateSelector";
    final GcpConnectorDTO gcpConnectorDTO =
        GcpConnectorDTO.builder()
            .credential(GcpConnectorCredentialDTO.builder()
                            .gcpCredentialType(GcpCredentialType.INHERIT_FROM_DELEGATE)
                            .config(GcpDelegateDetailsDTO.builder().delegateSelector(delegateSelector).build())
                            .build())
            .build();
    ConnectorDTO connectorDTO = createConnectorDTO(identifier, GCP, gcpConnectorDTO);
    connectorService.create(connectorDTO, accountIdentifier);
  }

  private ConnectorDTO createConnectorDTO(String identifier, ConnectorType type, ConnectorConfigDTO connectorConfig) {
    return ConnectorDTO.builder()
        .connectorInfo(ConnectorInfoDTO.builder()
                           .identifier(identifier)
                           .connectorType(type)
                           .connectorConfig(connectorConfig)
                           .build())
        .build();
  }

  private void createAWSConnectorWithInheritFromDelegate() {
    String delegateSelector = "delegateSelector";
    final AwsConnectorDTO awsCredentialDTO =
        AwsConnectorDTO.builder()
            .credential(AwsCredentialDTO.builder()
                            .awsCredentialType(AwsCredentialType.INHERIT_FROM_DELEGATE)
                            .crossAccountAccess(null)
                            .config(AwsInheritFromDelegateSpecDTO.builder().delegateSelector(delegateSelector).build())
                            .build())
            .build();
    ConnectorDTO connectorDTO = createConnectorDTO(identifier, AWS, awsCredentialDTO);
    connectorService.create(connectorDTO, accountIdentifier);
  }

  private void createK8sConnectorWithInheritFromDelegate() {
    String delegateName = "delegateName";
    KubernetesClusterConfigDTO connectorDTOWithDelegateCreds =
        KubernetesClusterConfigDTO.builder()
            .credential(KubernetesCredentialDTO.builder()
                            .kubernetesCredentialType(INHERIT_FROM_DELEGATE)
                            .config(KubernetesDelegateDetailsDTO.builder().delegateName(delegateName).build())
                            .build())
            .build();
    ConnectorDTO connectorDTO = createConnectorDTO(identifier, KUBERNETES_CLUSTER, connectorDTOWithDelegateCreds);
    connectorService.create(connectorDTO, accountIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void checkThatTheFieldWhichStoresCredentialTypeIsNotChanged() {
    assertThat(CREDENTIAL_TYPE_KEY).isEqualTo(KubernetesClusterConfigKeys.credentialType);
    assertThat(CREDENTIAL_TYPE_KEY).isEqualTo(AwsConfigKeys.credentialType);
    assertThat(CREDENTIAL_TYPE_KEY).isEqualTo(GcpConfigKeys.credentialType);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void checkThatTheInheritFromDelegateStringIsNotChanged() {
    assertThat(INHERIT_FROM_DELEGATE_STRING).isEqualTo(INHERIT_FROM_DELEGATE.name());
    assertThat(INHERIT_FROM_DELEGATE_STRING).isEqualTo(AwsCredentialType.INHERIT_FROM_DELEGATE.name());
    assertThat(INHERIT_FROM_DELEGATE_STRING).isEqualTo(GcpCredentialType.INHERIT_FROM_DELEGATE.name());
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void testListForWhenAFilterIsGiven() {
    String filterIdentifier = "filterIdentifier";
    List<String> names =
        Arrays.asList("docker test", "docker qa", "docker prod", "docker dev", "test docker", "docker qb", "dockerHub");
    List<String> identifiers =
        Arrays.asList("dockerTest", "dockerQa", "dockerProd", "dockerDev", "testDocker", "dockerQb", "dockerHub");
    List<String> descriptions =
        Arrays.asList("Docker Test", "Docker qa", "Docker prod", "Docker dev", "Test docker", "Docker qb", "DockerHub");
    createConnectorsWithGivenValues(names, identifiers, descriptions);

    ConnectorFilterDTO connectorFilterDTO =
        ConnectorFilterDTO.builder()
            .name("connectorFilter")
            .identifier(filterIdentifier)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .connectorNames(Arrays.asList("docker qa", "docker prod"))
            .connectorIdentifiers(Arrays.asList("dockerQa", "dockerProd", "dockerDev"))
            .descriptions(Arrays.asList("Docker"))
            .build();
    connectorFilterService.create(accountIdentifier, connectorFilterDTO);

    int numberOfConnectors = connectorService
                                 .list(0, 100, accountIdentifier,
                                     ConnectorListFilter.builder()
                                         .filterOrgIdentifier(orgIdentifier)
                                         .filterProjectIdentifier(projectIdentifier)
                                         .filterIdentifier(filterIdentifier)
                                         .build())
                                 .getNumberOfElements();
    assertThat(numberOfConnectors).isEqualTo(2);
  }

  private void createConnectorsWithGivenValues(
      List<String> nameList, List<String> identifierList, List<String> descriptionsList) {
    int size = nameList.size();
    for (int i = 0; i < size; i++) {
      ConnectorInfoDTO connectorInfoDTO = getConnector(nameList.get(i), identifierList.get(i), descriptionsList.get(i));
      connectorService.create(ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), accountIdentifier);
    }
  }
}
