package io.harness.connector;

import io.harness.connector.impl.ConnectorActivityServiceImpl;
import io.harness.connector.impl.ConnectorFilterServiceImpl;
import io.harness.connector.impl.ConnectorHeartbeatServiceImpl;
import io.harness.connector.impl.DefaultConnectorServiceImpl;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsDTOToEntity;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsEntityToDTO;
import io.harness.connector.mappers.artifactorymapper.ArtifactoryDTOToEntity;
import io.harness.connector.mappers.artifactorymapper.ArtifactoryEntityToDTO;
import io.harness.connector.mappers.awsmapper.AwsDTOToEntity;
import io.harness.connector.mappers.awsmapper.AwsEntityToDTO;
import io.harness.connector.mappers.docker.DockerDTOToEntity;
import io.harness.connector.mappers.docker.DockerEntityToDTO;
import io.harness.connector.mappers.gcpmappers.GcpDTOToEntity;
import io.harness.connector.mappers.gcpmappers.GcpEntityToDTO;
import io.harness.connector.mappers.gitconnectormapper.GitDTOToEntity;
import io.harness.connector.mappers.gitconnectormapper.GitEntityToDTO;
import io.harness.connector.mappers.githubconnector.GithubDTOToEntity;
import io.harness.connector.mappers.githubconnector.GithubEntityToDTO;
import io.harness.connector.mappers.gitlabconnector.GitlabDTOToEntity;
import io.harness.connector.mappers.gitlabconnector.GitlabEntityToDTO;
import io.harness.connector.mappers.jira.JiraDTOToEntity;
import io.harness.connector.mappers.jira.JiraEntityToDTO;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.connector.mappers.nexusmapper.NexusDTOToEntity;
import io.harness.connector.mappers.nexusmapper.NexusEntityToDTO;
import io.harness.connector.mappers.secretmanagermapper.GcpKmsDTOToEntity;
import io.harness.connector.mappers.secretmanagermapper.GcpKmsEntityToDTO;
import io.harness.connector.mappers.secretmanagermapper.LocalDTOToEntity;
import io.harness.connector.mappers.secretmanagermapper.LocalEntityToDTO;
import io.harness.connector.mappers.secretmanagermapper.VaultDTOToEntity;
import io.harness.connector.mappers.secretmanagermapper.VaultEntityToDTO;
import io.harness.connector.mappers.splunkconnectormapper.SplunkDTOToEntity;
import io.harness.connector.mappers.splunkconnectormapper.SplunkEntityToDTO;
import io.harness.connector.services.ConnectorActivityService;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.ArtifactoryConnectionValidator;
import io.harness.connector.validator.AwsConnectorValidator;
import io.harness.connector.validator.CVConnectorValidator;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.connector.validator.DockerConnectionValidator;
import io.harness.connector.validator.GcpConnectorValidator;
import io.harness.connector.validator.GitConnectorValidator;
import io.harness.connector.validator.JiraConnectorValidator;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.connector.validator.NexusConnectorValidator;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.persistence.HPersistence;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

public class ConnectorModule extends AbstractModule {
  public static final String DEFAULT_CONNECTOR_SERVICE = "defaultConnectorService";

  @Override
  protected void configure() {
    registerRequiredBindings();

    MapBinder<String, ConnectionValidator> connectorValidatorMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ConnectionValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesConnectionValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.GIT.getDisplayName()).to(GitConnectorValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.SPLUNK.getDisplayName()).to(CVConnectorValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.APP_DYNAMICS.getDisplayName()).to(CVConnectorValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.DOCKER.getDisplayName()).to(DockerConnectionValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.GCP.getDisplayName()).to(GcpConnectorValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.AWS.getDisplayName()).to(AwsConnectorValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.ARTIFACTORY.getDisplayName())
        .to(ArtifactoryConnectionValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.NEXUS.getDisplayName()).to(NexusConnectorValidator.class);
    connectorValidatorMapBinder.addBinding(ConnectorType.JIRA.getDisplayName()).to(JiraConnectorValidator.class);

    MapBinder<String, ConnectorDTOToEntityMapper> connectorDTOToEntityMapBinder =
        MapBinder.newMapBinder(binder(), String.class, ConnectorDTOToEntityMapper.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.GIT.getDisplayName()).to(GitDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.APP_DYNAMICS.getDisplayName())
        .to(AppDynamicsDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.SPLUNK.getDisplayName()).to(SplunkDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.VAULT.getDisplayName()).to(VaultDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.GCP_KMS.getDisplayName()).to(GcpKmsDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.LOCAL.getDisplayName()).to(LocalDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.DOCKER.getDisplayName()).to(DockerDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.GCP.getDisplayName()).to(GcpDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.AWS.getDisplayName()).to(AwsDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.ARTIFACTORY.getDisplayName())
        .to(ArtifactoryDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.JIRA.getDisplayName()).to(JiraDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.NEXUS.getDisplayName()).to(NexusDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.GITHUB.getDisplayName()).to(GithubDTOToEntity.class);
    connectorDTOToEntityMapBinder.addBinding(ConnectorType.GITLAB.getDisplayName()).to(GitlabDTOToEntity.class);

    MapBinder<String, ConnectorEntityToDTOMapper> connectorEntityToDTOMapper =
        MapBinder.newMapBinder(binder(), String.class, ConnectorEntityToDTOMapper.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.KUBERNETES_CLUSTER.getDisplayName())
        .to(KubernetesEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.GIT.getDisplayName()).to(GitEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.APP_DYNAMICS.getDisplayName()).to(AppDynamicsEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.SPLUNK.getDisplayName()).to(SplunkEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.VAULT.getDisplayName()).to(VaultEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.GCP_KMS.getDisplayName()).to(GcpKmsEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.LOCAL.getDisplayName()).to(LocalEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.DOCKER.getDisplayName()).to(DockerEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.GCP.getDisplayName()).to(GcpEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.AWS.getDisplayName()).to(AwsEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.ARTIFACTORY.getDisplayName()).to(ArtifactoryEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.JIRA.getDisplayName()).to(JiraEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.NEXUS.getDisplayName()).to(NexusEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.GITHUB.getDisplayName()).to(GithubEntityToDTO.class);
    connectorEntityToDTOMapper.addBinding(ConnectorType.GITLAB.getDisplayName()).to(GitlabEntityToDTO.class);

    bind(ConnectorService.class)
        .annotatedWith(Names.named(DEFAULT_CONNECTOR_SERVICE))
        .to(DefaultConnectorServiceImpl.class);
    bind(ConnectorActivityService.class).to(ConnectorActivityServiceImpl.class);
    bind(ConnectorFilterService.class).to(ConnectorFilterServiceImpl.class);
    bind(ConnectorHeartbeatService.class).to(ConnectorHeartbeatServiceImpl.class);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
  }
}