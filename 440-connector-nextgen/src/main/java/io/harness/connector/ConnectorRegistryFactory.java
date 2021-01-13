package io.harness.connector;

import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsDTOToEntity;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsEntityToDTO;
import io.harness.connector.mappers.awsmapper.AwsDTOToEntity;
import io.harness.connector.mappers.awsmapper.AwsEntityToDTO;
import io.harness.connector.mappers.bitbucketconnectormapper.BitbucketDTOToEntity;
import io.harness.connector.mappers.bitbucketconnectormapper.BitbucketEntityToDTO;
import io.harness.connector.mappers.ceawsmapper.CEAwsDTOToEntity;
import io.harness.connector.mappers.ceawsmapper.CEAwsEntityToDTO;
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
import io.harness.connector.validator.AbstractConnectorValidator;
import io.harness.connector.validator.AlwaysTrueConnectorValidator;
import io.harness.connector.validator.ArtifactoryConnectionValidator;
import io.harness.connector.validator.AwsConnectorValidator;
import io.harness.connector.validator.BitbucketConnectorValidator;
import io.harness.connector.validator.CEAwsConnectorValidator;
import io.harness.connector.validator.CVConnectorValidator;
import io.harness.connector.validator.DockerConnectionValidator;
import io.harness.connector.validator.GcpConnectorValidator;
import io.harness.connector.validator.GitConnectorValidator;
import io.harness.connector.validator.GithubConnectorValidator;
import io.harness.connector.validator.GitlabConnectorValidator;
import io.harness.connector.validator.JiraConnectorValidator;
import io.harness.connector.validator.KubernetesConnectionValidator;
import io.harness.connector.validator.NexusConnectorValidator;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.task.docker.DockerTestConnectionDelegateTask;
import io.harness.delegate.task.git.NGGitCommandTask;
import io.harness.delegate.task.k8s.ConnectorValidationHandler;
import io.harness.delegate.task.k8s.KubernetesTestConnectionDelegateTask;

import java.util.HashMap;
import java.util.Map;

public class ConnectorRegistryFactory {
  private static Map<ConnectorType, ConnectorRegistrar> registrar = new HashMap<>();

  static {
    registrar.put(ConnectorType.KUBERNETES_CLUSTER,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_PROVIDER, KubernetesConnectionValidator.class,
            KubernetesTestConnectionDelegateTask.KubernetesValidationHandler.class, KubernetesDTOToEntity.class,
            KubernetesEntityToDTO.class));
    registrar.put(ConnectorType.GIT,
        new ConnectorRegistrar(ConnectorCategory.CODE_REPO, GitConnectorValidator.class,
            NGGitCommandTask.GitValidationHandler.class, GitDTOToEntity.class, GitEntityToDTO.class));
    registrar.put(ConnectorType.APP_DYNAMICS,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            NoOpConnectorValidationHandler.class, AppDynamicsDTOToEntity.class, AppDynamicsEntityToDTO.class));
    registrar.put(ConnectorType.SPLUNK,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            NoOpConnectorValidationHandler.class, SplunkDTOToEntity.class, SplunkEntityToDTO.class));
    registrar.put(ConnectorType.VAULT,
        new ConnectorRegistrar(ConnectorCategory.SECRET_MANAGER, AlwaysTrueConnectorValidator.class,
            NoOpConnectorValidationHandler.class, VaultDTOToEntity.class, VaultEntityToDTO.class));
    registrar.put(ConnectorType.GCP_KMS,
        new ConnectorRegistrar(ConnectorCategory.SECRET_MANAGER, AlwaysTrueConnectorValidator.class,
            NoOpConnectorValidationHandler.class, GcpKmsDTOToEntity.class, GcpKmsEntityToDTO.class));
    registrar.put(ConnectorType.LOCAL,
        new ConnectorRegistrar(ConnectorCategory.SECRET_MANAGER, AlwaysTrueConnectorValidator.class,
            NoOpConnectorValidationHandler.class, LocalDTOToEntity.class, LocalEntityToDTO.class));

    registrar.put(ConnectorType.DOCKER,
        new ConnectorRegistrar(ConnectorCategory.ARTIFACTORY, DockerConnectionValidator.class,
            DockerTestConnectionDelegateTask.DockerValidationHandler.class, DockerDTOToEntity.class,
            DockerEntityToDTO.class));

    registrar.put(ConnectorType.GCP,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_PROVIDER, GcpConnectorValidator.class,
            NoOpConnectorValidationHandler.class, GcpDTOToEntity.class, GcpEntityToDTO.class));
    registrar.put(ConnectorType.AWS,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_PROVIDER, AwsConnectorValidator.class,
            NoOpConnectorValidationHandler.class, AwsDTOToEntity.class, AwsEntityToDTO.class));
    registrar.put(ConnectorType.CE_AWS,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_COST, CEAwsConnectorValidator.class,
            NoOpConnectorValidationHandler.class, CEAwsDTOToEntity.class, CEAwsEntityToDTO.class));
    registrar.put(ConnectorType.ARTIFACTORY,
        new ConnectorRegistrar(ConnectorCategory.ARTIFACTORY, ArtifactoryConnectionValidator.class,
            NoOpConnectorValidationHandler.class, CEAwsDTOToEntity.class, CEAwsEntityToDTO.class));
    registrar.put(ConnectorType.JIRA,
        new ConnectorRegistrar(ConnectorCategory.TICKETING, JiraConnectorValidator.class,
            NoOpConnectorValidationHandler.class, JiraDTOToEntity.class, JiraEntityToDTO.class));
    registrar.put(ConnectorType.NEXUS,
        new ConnectorRegistrar(ConnectorCategory.ARTIFACTORY, NexusConnectorValidator.class,
            NoOpConnectorValidationHandler.class, NexusDTOToEntity.class, NexusEntityToDTO.class));
    registrar.put(ConnectorType.GITHUB,
        new ConnectorRegistrar(ConnectorCategory.CODE_REPO, GithubConnectorValidator.class,
            NoOpConnectorValidationHandler.class, GithubDTOToEntity.class, GithubEntityToDTO.class));
    registrar.put(ConnectorType.GITLAB,
        new ConnectorRegistrar(ConnectorCategory.CODE_REPO, GitlabConnectorValidator.class,
            NoOpConnectorValidationHandler.class, GitlabDTOToEntity.class, GitlabEntityToDTO.class));
    registrar.put(ConnectorType.BITBUCKET,
        new ConnectorRegistrar(ConnectorCategory.CODE_REPO, BitbucketConnectorValidator.class,
            NoOpConnectorValidationHandler.class, BitbucketDTOToEntity.class, BitbucketEntityToDTO.class));
  }

  public static Class<? extends AbstractConnectorValidator> getConnectorValidator(ConnectorType connectorType) {
    return registrar.get(connectorType).getConnectorValidator();
  }

  public static Class<? extends ConnectorValidationHandler> getConnectorValidationHandler(ConnectorType connectorType) {
    return registrar.get(connectorType).getConnectorValidationHandler();
  }

  public static ConnectorCategory getConnectorCategory(ConnectorType connectorType) {
    return registrar.get(connectorType).getConnectorCategory();
  }
  public static Class<? extends ConnectorDTOToEntityMapper<?, ?>> getConnectorDTOToEntityMapper(
      ConnectorType connectorType) {
    return registrar.get(connectorType).getConnectorDTOToEntityMapper();
  }

  public static Class<? extends ConnectorEntityToDTOMapper<?, ?>> getConnectorEntityToDTOMapper(
      ConnectorType connectorType) {
    return registrar.get(connectorType).getConnectorEntityToDTOMapper();
  }
}
