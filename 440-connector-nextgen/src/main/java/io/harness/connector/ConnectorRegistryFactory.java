package io.harness.connector;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.heartbeat.*;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsDTOToEntity;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsEntityToDTO;
import io.harness.connector.mappers.artifactorymapper.ArtifactoryDTOToEntity;
import io.harness.connector.mappers.artifactorymapper.ArtifactoryEntityToDTO;
import io.harness.connector.mappers.awscodecommit.AwsCodeCommitDTOToEntity;
import io.harness.connector.mappers.awscodecommit.AwsCodeCommitEntityToDTO;
import io.harness.connector.mappers.awsmapper.AwsDTOToEntity;
import io.harness.connector.mappers.awsmapper.AwsEntityToDTO;
import io.harness.connector.mappers.bitbucketconnectormapper.BitbucketDTOToEntity;
import io.harness.connector.mappers.bitbucketconnectormapper.BitbucketEntityToDTO;
import io.harness.connector.mappers.ceawsmapper.CEAwsDTOToEntity;
import io.harness.connector.mappers.ceawsmapper.CEAwsEntityToDTO;
import io.harness.connector.mappers.ceazure.CEAzureDTOToEntity;
import io.harness.connector.mappers.ceazure.CEAzureEntityToDTO;
import io.harness.connector.mappers.cek8s.CEKubernetesDTOToEntity;
import io.harness.connector.mappers.cek8s.CEKubernetesEntityToDTO;
import io.harness.connector.mappers.datadogmapper.DatadogDTOToEntity;
import io.harness.connector.mappers.datadogmapper.DatadogEntityToDTO;
import io.harness.connector.mappers.docker.DockerDTOToEntity;
import io.harness.connector.mappers.docker.DockerEntityToDTO;
import io.harness.connector.mappers.dynatracemapper.DynatraceDTOToEntity;
import io.harness.connector.mappers.dynatracemapper.DynatraceEntityToDTO;
import io.harness.connector.mappers.gcpcloudcost.GcpCloudCostDTOToEntity;
import io.harness.connector.mappers.gcpcloudcost.GcpCloudCostEntityToDTO;
import io.harness.connector.mappers.gcpmappers.GcpDTOToEntity;
import io.harness.connector.mappers.gcpmappers.GcpEntityToDTO;
import io.harness.connector.mappers.gitconnectormapper.GitDTOToEntity;
import io.harness.connector.mappers.gitconnectormapper.GitEntityToDTO;
import io.harness.connector.mappers.githubconnector.GithubDTOToEntity;
import io.harness.connector.mappers.githubconnector.GithubEntityToDTO;
import io.harness.connector.mappers.gitlabconnector.GitlabDTOToEntity;
import io.harness.connector.mappers.gitlabconnector.GitlabEntityToDTO;
import io.harness.connector.mappers.helm.HttpHelmDTOToEntity;
import io.harness.connector.mappers.helm.HttpHelmEntityToDTO;
import io.harness.connector.mappers.jira.JiraDTOToEntity;
import io.harness.connector.mappers.jira.JiraEntityToDTO;
import io.harness.connector.mappers.kubernetesMapper.KubernetesDTOToEntity;
import io.harness.connector.mappers.kubernetesMapper.KubernetesEntityToDTO;
import io.harness.connector.mappers.newerlicmapper.NewRelicDTOToEntity;
import io.harness.connector.mappers.newerlicmapper.NewRelicEntityToDTO;
import io.harness.connector.mappers.nexusmapper.NexusDTOToEntity;
import io.harness.connector.mappers.nexusmapper.NexusEntityToDTO;
import io.harness.connector.mappers.pagerduty.PagerDutyDTOToEntity;
import io.harness.connector.mappers.pagerduty.PagerDutyEntityToDTO;
import io.harness.connector.mappers.prometheusmapper.PrometheusDTOToEntity;
import io.harness.connector.mappers.prometheusmapper.PrometheusEntityToDTO;
import io.harness.connector.mappers.secretmanagermapper.*;
import io.harness.connector.mappers.splunkconnectormapper.SplunkDTOToEntity;
import io.harness.connector.mappers.splunkconnectormapper.SplunkEntityToDTO;
import io.harness.connector.mappers.sumologicmapper.SumoLogicDTOToEntity;
import io.harness.connector.mappers.sumologicmapper.SumoLogicEntityToDTO;
import io.harness.connector.task.git.GitValidationHandlerViaManager;
import io.harness.connector.validator.*;
import io.harness.connector.validator.scmValidators.*;
import io.harness.delegate.beans.connector.ConnectorType;

import java.util.HashMap;
import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.DX;

@OwnedBy(DX)
public class ConnectorRegistryFactory {
  private static Map<ConnectorType, ConnectorRegistrar> registrar = new HashMap<>();

  static {
    registrar.put(ConnectorType.KUBERNETES_CLUSTER,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_PROVIDER, KubernetesConnectionValidator.class,
            K8sConnectorValidationParamsProvider.class, KubernetesDTOToEntity.class, KubernetesEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.CE_KUBERNETES_CLUSTER,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_COST, CEKubernetesConnectionValidator.class,
            CEK8sConnectorValidationParamsProvider.class, CEKubernetesDTOToEntity.class,
            CEKubernetesEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.GIT,
        new ConnectorRegistrar(ConnectorCategory.CODE_REPO, GitConnectorValidator.class,
            ScmConnectorValidationParamsProvider.class, GitDTOToEntity.class, GitEntityToDTO.class, GitValidationHandlerViaManager.class));
    registrar.put(ConnectorType.APP_DYNAMICS,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            CVConnectorParamsProvider.class, AppDynamicsDTOToEntity.class, AppDynamicsEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.NEW_RELIC,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            CVConnectorParamsProvider.class, NewRelicDTOToEntity.class, NewRelicEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.DATADOG,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            CVConnectorParamsProvider.class, DatadogDTOToEntity.class, DatadogEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.SPLUNK,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            CVConnectorParamsProvider.class, SplunkDTOToEntity.class, SplunkEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.PROMETHEUS,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            CVConnectorParamsProvider.class, PrometheusDTOToEntity.class, PrometheusEntityToDTO.class ,NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.SUMOLOGIC,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            CVConnectorParamsProvider.class, SumoLogicDTOToEntity.class, SumoLogicEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.DYNATRACE,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            CVConnectorParamsProvider.class, DynatraceDTOToEntity.class, DynatraceEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.VAULT,
        new ConnectorRegistrar(ConnectorCategory.SECRET_MANAGER, SecretManagerConnectorValidator.class,
            VaultConnectorValidationParamsProvider.class, VaultDTOToEntity.class, VaultEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.AZURE_KEY_VAULT,
        new ConnectorRegistrar(ConnectorCategory.SECRET_MANAGER, SecretManagerConnectorValidator.class,
            AzureKeyVaultConnectorValidationParamsProvider.class, AzureKeyVaultDTOToEntity.class,
            AzureKeyVaultEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.GCP_KMS,
        new ConnectorRegistrar(ConnectorCategory.SECRET_MANAGER, SecretManagerConnectorValidator.class,
            GcpKmsConnectorValidationParamsProvider.class, GcpKmsDTOToEntity.class, GcpKmsEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.AWS_KMS,
        new ConnectorRegistrar(ConnectorCategory.SECRET_MANAGER, SecretManagerConnectorValidator.class,
            AwsKmsConnectorValidationParamsProvider.class, AwsKmsDTOToEntity.class, AwsKmsEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.AWS_SECRET_MANAGER,
        new ConnectorRegistrar(ConnectorCategory.SECRET_MANAGER, SecretManagerConnectorValidator.class,
            AwsSecretManagerValidationParamsProvider.class, AwsSecretManagerDTOToEntity.class,
            AwsSecretManagerEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.LOCAL,
        new ConnectorRegistrar(ConnectorCategory.SECRET_MANAGER, SecretManagerConnectorValidator.class,
            NoOpConnectorValidationParamsProvider.class, LocalDTOToEntity.class, LocalEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.DOCKER,
        new ConnectorRegistrar(ConnectorCategory.ARTIFACTORY, DockerConnectionValidator.class,
            DockerConnectorValidationParamsProvider.class, DockerDTOToEntity.class, DockerEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.GCP,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_PROVIDER, GcpConnectorValidator.class,
            GcpValidationParamsProvider.class, GcpDTOToEntity.class, GcpEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.AWS,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_PROVIDER, AwsConnectorValidator.class,
            AwsValidationParamsProvider.class, AwsDTOToEntity.class, AwsEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.CE_AWS,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_COST, CEAwsConnectorValidator.class,
            NoOpConnectorValidationParamsProvider.class, CEAwsDTOToEntity.class, CEAwsEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.ARTIFACTORY,
        new ConnectorRegistrar(ConnectorCategory.ARTIFACTORY, ArtifactoryConnectionValidator.class,
            ArtifactoryValidationParamsProvider.class, ArtifactoryDTOToEntity.class, ArtifactoryEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.JIRA,
        new ConnectorRegistrar(ConnectorCategory.TICKETING, JiraConnectorValidator.class,
            NoOpConnectorValidationParamsProvider.class, JiraDTOToEntity.class, JiraEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.NEXUS,
        new ConnectorRegistrar(ConnectorCategory.ARTIFACTORY, NexusConnectorValidator.class,
            NexusValidationParamsProvider.class, NexusDTOToEntity.class, NexusEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.GITHUB,
        new ConnectorRegistrar(ConnectorCategory.CODE_REPO, GithubConnectorValidator.class,
            ScmConnectorValidationParamsProvider.class, GithubDTOToEntity.class, GithubEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.GITLAB,
        new ConnectorRegistrar(ConnectorCategory.CODE_REPO, GitlabConnectorValidator.class,
            ScmConnectorValidationParamsProvider.class, GitlabDTOToEntity.class, GitlabEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.BITBUCKET,
        new ConnectorRegistrar(ConnectorCategory.CODE_REPO, BitbucketConnectorValidator.class,
            ScmConnectorValidationParamsProvider.class, BitbucketDTOToEntity.class, BitbucketEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.CODECOMMIT,
        new ConnectorRegistrar(ConnectorCategory.CODE_REPO, AwsCodeCommitValidator.class,
            NoOpConnectorValidationParamsProvider.class, AwsCodeCommitDTOToEntity.class,
            AwsCodeCommitEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.CE_AZURE,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_COST, CEAzureConnectorValidator.class,
            NoOpConnectorValidationParamsProvider.class, CEAzureDTOToEntity.class, CEAzureEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.GCP_CLOUD_COST,
        new ConnectorRegistrar(ConnectorCategory.CLOUD_COST, CEGcpConnectorValidator.class,
            NoOpConnectorValidationParamsProvider.class, GcpCloudCostDTOToEntity.class, GcpCloudCostEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.HTTP_HELM_REPO,
        new ConnectorRegistrar(ConnectorCategory.ARTIFACTORY, HttpHelmRepoConnectionValidator.class,
            HttpHelmConnectorValidationParamsProvider.class, HttpHelmDTOToEntity.class, HttpHelmEntityToDTO.class, NoOpConnectorValidationHandler.class));
    registrar.put(ConnectorType.PAGER_DUTY,
        new ConnectorRegistrar(ConnectorCategory.MONITORING, CVConnectorValidator.class,
            CVConnectorParamsProvider.class, PagerDutyDTOToEntity.class, PagerDutyEntityToDTO.class, NoOpConnectorValidationHandler.class));
  }

  public static Class<? extends ConnectionValidator> getConnectorValidator(ConnectorType connectorType) {
    return registrar.get(connectorType).getConnectorValidator();
  }

  public static Class<? extends ConnectorValidationParamsProvider> getConnectorValidationParamsProvider(
      ConnectorType connectorType) {
    return registrar.get(connectorType).getConnectorValidationParams();
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

  public static Class<? extends ConnectorValidationHandler> getConnectorValidationHandler(
          ConnectorType connectorType) {
    return registrar.get(connectorType).getConnectorValidationHandler();
  }

}
