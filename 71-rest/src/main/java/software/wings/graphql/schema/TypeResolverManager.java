package software.wings.graphql.schema;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import graphql.schema.TypeResolver;
import lombok.experimental.UtilityClass;
import software.wings.graphql.schema.type.QLDeploymentOutcome;
import software.wings.graphql.schema.type.QLExecutedAlongPipeline;
import software.wings.graphql.schema.type.QLExecutedByTrigger;
import software.wings.graphql.schema.type.QLExecutedByUser;
import software.wings.graphql.schema.type.QLPipelineExecution;
import software.wings.graphql.schema.type.QLWorkflowExecution;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLStackedData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.audit.QLApiKeyChangeSet;
import software.wings.graphql.schema.type.audit.QLGenericChangeSet;
import software.wings.graphql.schema.type.audit.QLGitChangeSet;
import software.wings.graphql.schema.type.audit.QLUserChangeSet;
import software.wings.graphql.schema.type.cloudProvider.QLAwsCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLAzureCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLGcpCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLKubernetesClusterCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPcfCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLPhysicalDataCenterCloudProvider;
import software.wings.graphql.schema.type.connector.QLAmazonS3HelmConnector;
import software.wings.graphql.schema.type.connector.QLAmazonS3RepoConnector;
import software.wings.graphql.schema.type.connector.QLApmVerificationConnector;
import software.wings.graphql.schema.type.connector.QLAppDynamicsConnector;
import software.wings.graphql.schema.type.connector.QLArtifactoryConnector;
import software.wings.graphql.schema.type.connector.QLBambooConnector;
import software.wings.graphql.schema.type.connector.QLBugSnagConnector;
import software.wings.graphql.schema.type.connector.QLDataDogConnector;
import software.wings.graphql.schema.type.connector.QLDockerConnector;
import software.wings.graphql.schema.type.connector.QLDynaTraceConnector;
import software.wings.graphql.schema.type.connector.QLECRConnector;
import software.wings.graphql.schema.type.connector.QLElbConnector;
import software.wings.graphql.schema.type.connector.QLElkConnector;
import software.wings.graphql.schema.type.connector.QLGCRConnector;
import software.wings.graphql.schema.type.connector.QLGCSConnector;
import software.wings.graphql.schema.type.connector.QLGCSHelmRepoConnector;
import software.wings.graphql.schema.type.connector.QLGitConnector;
import software.wings.graphql.schema.type.connector.QLHttpHelmRepoConnector;
import software.wings.graphql.schema.type.connector.QLInstanaConnector;
import software.wings.graphql.schema.type.connector.QLJenkinsConnector;
import software.wings.graphql.schema.type.connector.QLJiraConnector;
import software.wings.graphql.schema.type.connector.QLLogzConnector;
import software.wings.graphql.schema.type.connector.QLNewRelicConnector;
import software.wings.graphql.schema.type.connector.QLNexusConnector;
import software.wings.graphql.schema.type.connector.QLPrometheusConnector;
import software.wings.graphql.schema.type.connector.QLSMBConnector;
import software.wings.graphql.schema.type.connector.QLServiceNowConnector;
import software.wings.graphql.schema.type.connector.QLSftpConnector;
import software.wings.graphql.schema.type.connector.QLSlackConnector;
import software.wings.graphql.schema.type.connector.QLSmtpConnector;
import software.wings.graphql.schema.type.connector.QLSplunkConnector;
import software.wings.graphql.schema.type.connector.QLSumoConnector;
import software.wings.graphql.schema.type.instance.QLAutoScalingGroupInstance;
import software.wings.graphql.schema.type.instance.QLCodeDeployInstance;
import software.wings.graphql.schema.type.instance.QLEc2Instance;
import software.wings.graphql.schema.type.instance.QLEcsContainerInstance;
import software.wings.graphql.schema.type.instance.QLK8SPodInstance;
import software.wings.graphql.schema.type.instance.QLPcfInstance;
import software.wings.graphql.schema.type.instance.QLPhysicalHostInstance;
import software.wings.graphql.schema.type.secrets.QLEncryptedText;
import software.wings.graphql.schema.type.secrets.QLWinRMCredential;
import software.wings.graphql.schema.type.usergroup.QLLDAPSettings;
import software.wings.graphql.schema.type.usergroup.QLSAMLSettings;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@Singleton
public class TypeResolverManager {
  // Uniface is a short for union or interface
  @UtilityClass
  public static final class TypeResolverManagerUnifaces {
    public static final String Cause = "Cause";
    public static final String CloudProvider = "CloudProvider";
    public static final String Connector = "Connector";
    public static final String Data = "Data";
    public static final String Execution = "Execution";
    public static final String Instance = "Instance";
    public static final String Outcome = "Outcome";
    public static final String PhysicalInstance = "PhysicalInstance";
    public static final String ChangeSet = "ChangeSet";
    public static final String LinkedSSOSetting = "LinkedSSOSetting";
    public static final String Secret = "Secret";
  }

  @UtilityClass
  public static final class TypeResolverManagerTypes {
    public static final String AggregatedData = "AggregatedData";
    public static final String AutoScalingGroupInstance = "AutoScalingGroupInstance";
    public static final String AwsCloudProvider = "AwsCloudProvider";
    public static final String AzureCloudProvider = "AzureCloudProvider";
    public static final String CodeDeployInstance = "CodeDeployInstance";
    public static final String DeploymentOutcome = "DeploymentOutcome";
    public static final String Ec2Instance = "Ec2Instance";
    public static final String EcsContainerInstance = "EcsContainerInstance";
    public static final String ExecutedAlongPipeline = "ExecutedAlongPipeline";
    public static final String ExecutedByTrigger = "ExecutedByTrigger";
    public static final String ExecutedByUser = "ExecutedByUser";
    public static final String GcpCloudProvider = "GcpCloudProvider";
    public static final String K8sPodInstance = "K8sPodInstance";
    public static final String KubernetesCloudProvider = "KubernetesCloudProvider";
    public static final String PcfCloudProvider = "PcfCloudProvider";
    public static final String PcfInstance = "PcfInstance";
    public static final String PhysicalDataCenterCloudProvider = "PhysicalDataCenterCloudProvider";
    public static final String PhysicalHostInstance = "PhysicalHostInstance";
    public static final String PipelineExecution = "PipelineExecution";
    public static final String SinglePointData = "SinglePointData";
    public static final String StackedData = "StackedData";
    public static final String StackedTimeSeriesData = "StackedTimeSeriesData";
    public static final String TimeSeriesData = "TimeSeriesData";
    public static final String WorkflowExecution = "WorkflowExecution";

    public static final String AmazonS3Connector = "AmazonS3Connector";
    public static final String ApmVerificationConnector = "ApmVerificationConnector";
    public static final String AppDynamicsConnector = "AppDynamicsConnector";
    public static final String InstanaCollector = "InstanaConnector";
    public static final String ArtifactoryConnector = "ArtifactoryConnector";
    public static final String BambooConnector = "BambooConnector";
    public static final String BugSnagConnector = "BugSnagConnector";
    public static final String DataDogConnector = "DataDogConnector";
    public static final String DockerConnector = "DockerConnector";
    public static final String DynaTraceConnector = "DynaTraceConnector";
    public static final String ECRConnector = "ECRConnector";
    public static final String ElbConnector = "ElbConnector";
    public static final String ElkConnector = "ElkConnector";
    public static final String GCRConnector = "GCRConnector";
    public static final String GCSConnector = "GCSConnector";
    public static final String GitConnector = "GitConnector";
    public static final String JenkinsConnector = "JenkinsConnector";
    public static final String JiraConnector = "JiraConnector";
    public static final String LogzConnector = "LogzConnector";
    public static final String NewRelicConnector = "NewRelicConnector";
    public static final String NexusConnector = "NexusConnector";
    public static final String PrometheusConnector = "PrometheusConnector";
    public static final String ServiceNowConnector = "ServiceNowConnector";
    public static final String SftpConnector = "SftpConnector";
    public static final String SlackConnector = "SlackConnector";
    public static final String SMBConnector = "SMBConnector";
    public static final String SmtpConnector = "SmtpConnector";
    public static final String SplunkConnector = "SplunkConnector";
    public static final String SumoConnector = "SumoConnector";
    public static final String GCSHelmRepoConnector = "GCSHelmRepoConnector";
    public static final String HttpHelmRepoConnector = "HttpHelmRepoConnector";
    public static final String AmazonS3HelmRepoConnector = "AmazonS3HelmRepoConnector";
    public static final String UserChangeSet = "UserChangeSet";
    public static final String GitChangeSet = "GitChangeSet";
    public static final String ApiKeyChangeSet = "ApiKeyChangeSet";
    public static final String GenericChangeSet = "GenericChangeSet";
    public static final String ldapSettings = "LDAPSettings";
    public static final String samlSettings = "SAMLSettings";
    public static final String encryptedText = "EncryptedText";
    public static final String winRMCredential = "WinRMCredential";
  }

  /**
   * Later, we should have TEST to make sure a fieldName is only used once
   * otherwise it may be overridden.
   * @return
   */
  public Map<String, TypeResolver> getTypeResolverMap() {
    return ImmutableMap.<String, TypeResolver>builder()
        .put(TypeResolverManagerUnifaces.Cause,
            getResultTypeResolver(
                ImmutableMap.<Class, String>builder()
                    .put(QLExecutedByUser.class, TypeResolverManagerTypes.ExecutedByUser)
                    .put(QLExecutedAlongPipeline.class, TypeResolverManagerTypes.ExecutedAlongPipeline)
                    .put(QLExecutedByTrigger.class, TypeResolverManagerTypes.ExecutedByTrigger)
                    .build()))
        .put(TypeResolverManagerUnifaces.CloudProvider,
            getResultTypeResolver(
                ImmutableMap.<Class, String>builder()
                    .put(QLAwsCloudProvider.class, TypeResolverManagerTypes.AwsCloudProvider)
                    .put(QLPhysicalDataCenterCloudProvider.class,
                        TypeResolverManagerTypes.PhysicalDataCenterCloudProvider)
                    .put(QLAzureCloudProvider.class, TypeResolverManagerTypes.AzureCloudProvider)
                    .put(QLGcpCloudProvider.class, TypeResolverManagerTypes.GcpCloudProvider)
                    .put(QLKubernetesClusterCloudProvider.class, TypeResolverManagerTypes.KubernetesCloudProvider)
                    .put(QLPcfCloudProvider.class, TypeResolverManagerTypes.PcfCloudProvider)
                    .build()))
        .put(TypeResolverManagerUnifaces.Connector,
            getResultTypeResolver(
                ImmutableMap.<Class, String>builder()
                    .put(QLAmazonS3RepoConnector.class, TypeResolverManagerTypes.AmazonS3Connector)
                    .put(QLApmVerificationConnector.class, TypeResolverManagerTypes.ApmVerificationConnector)
                    .put(QLAppDynamicsConnector.class, TypeResolverManagerTypes.AppDynamicsConnector)
                    .put(QLArtifactoryConnector.class, TypeResolverManagerTypes.ArtifactoryConnector)
                    .put(QLBambooConnector.class, TypeResolverManagerTypes.BambooConnector)
                    .put(QLBugSnagConnector.class, TypeResolverManagerTypes.BugSnagConnector)
                    .put(QLDataDogConnector.class, TypeResolverManagerTypes.DataDogConnector)
                    .put(QLDockerConnector.class, TypeResolverManagerTypes.DockerConnector)
                    .put(QLDynaTraceConnector.class, TypeResolverManagerTypes.DynaTraceConnector)
                    .put(QLECRConnector.class, TypeResolverManagerTypes.ECRConnector)
                    .put(QLElbConnector.class, TypeResolverManagerTypes.ElbConnector)
                    .put(QLElkConnector.class, TypeResolverManagerTypes.ElkConnector)
                    .put(QLGCRConnector.class, TypeResolverManagerTypes.GCRConnector)
                    .put(QLGCSConnector.class, TypeResolverManagerTypes.GCSConnector)
                    .put(QLGitConnector.class, TypeResolverManagerTypes.GitConnector)
                    .put(QLJenkinsConnector.class, TypeResolverManagerTypes.JenkinsConnector)
                    .put(QLJiraConnector.class, TypeResolverManagerTypes.JiraConnector)
                    .put(QLLogzConnector.class, TypeResolverManagerTypes.LogzConnector)
                    .put(QLNewRelicConnector.class, TypeResolverManagerTypes.NewRelicConnector)
                    .put(QLNexusConnector.class, TypeResolverManagerTypes.NexusConnector)
                    .put(QLPrometheusConnector.class, TypeResolverManagerTypes.PrometheusConnector)
                    .put(QLServiceNowConnector.class, TypeResolverManagerTypes.ServiceNowConnector)
                    .put(QLSftpConnector.class, TypeResolverManagerTypes.SftpConnector)
                    .put(QLSlackConnector.class, TypeResolverManagerTypes.SlackConnector)
                    .put(QLSMBConnector.class, TypeResolverManagerTypes.SMBConnector)
                    .put(QLSmtpConnector.class, TypeResolverManagerTypes.SmtpConnector)
                    .put(QLSplunkConnector.class, TypeResolverManagerTypes.SplunkConnector)
                    .put(QLSumoConnector.class, TypeResolverManagerTypes.SumoConnector)
                    .put(QLInstanaConnector.class, TypeResolverManagerTypes.InstanaCollector)
                    .put(QLGCSHelmRepoConnector.class, TypeResolverManagerTypes.GCSHelmRepoConnector)
                    .put(QLHttpHelmRepoConnector.class, TypeResolverManagerTypes.HttpHelmRepoConnector)
                    .put(QLAmazonS3HelmConnector.class, TypeResolverManagerTypes.AmazonS3HelmRepoConnector)
                    .build()))
        .put(TypeResolverManagerUnifaces.Execution,
            getResultTypeResolver(ImmutableMap.<Class, String>builder()
                                      .put(QLPipelineExecution.class, TypeResolverManagerTypes.PipelineExecution)
                                      .put(QLWorkflowExecution.class, TypeResolverManagerTypes.WorkflowExecution)
                                      .build()))
        .put(TypeResolverManagerUnifaces.Instance,
            getResultTypeResolver(
                ImmutableMap.<Class, String>builder()
                    .put(QLPhysicalHostInstance.class, TypeResolverManagerTypes.PhysicalHostInstance)
                    .put(QLK8SPodInstance.class, TypeResolverManagerTypes.K8sPodInstance)
                    .put(QLEc2Instance.class, TypeResolverManagerTypes.Ec2Instance)
                    .put(QLPcfInstance.class, TypeResolverManagerTypes.PcfInstance)
                    .put(QLCodeDeployInstance.class, TypeResolverManagerTypes.CodeDeployInstance)
                    .put(QLEcsContainerInstance.class, TypeResolverManagerTypes.EcsContainerInstance)
                    .put(QLAutoScalingGroupInstance.class, TypeResolverManagerTypes.AutoScalingGroupInstance)
                    .build()))
        .put(TypeResolverManagerUnifaces.Outcome,
            getResultTypeResolver(ImmutableMap.<Class, String>builder()
                                      .put(QLDeploymentOutcome.class, TypeResolverManagerTypes.DeploymentOutcome)
                                      .build()))
        .put(TypeResolverManagerUnifaces.PhysicalInstance,
            getResultTypeResolver(
                ImmutableMap.<Class, String>builder()
                    .put(QLPhysicalHostInstance.class, TypeResolverManagerTypes.PhysicalHostInstance)
                    .put(QLEc2Instance.class, TypeResolverManagerTypes.Ec2Instance)
                    .put(QLCodeDeployInstance.class, TypeResolverManagerTypes.CodeDeployInstance)
                    .put(QLAutoScalingGroupInstance.class, TypeResolverManagerTypes.AutoScalingGroupInstance)
                    .build()))
        .put(TypeResolverManagerUnifaces.Data,
            getResultTypeResolver(
                ImmutableMap.<Class, String>builder()
                    .put(QLAggregatedData.class, TypeResolverManagerTypes.AggregatedData)
                    .put(QLSinglePointData.class, TypeResolverManagerTypes.SinglePointData)
                    .put(QLStackedData.class, TypeResolverManagerTypes.StackedData)
                    .put(QLTimeSeriesData.class, TypeResolverManagerTypes.TimeSeriesData)
                    .put(QLStackedTimeSeriesData.class, TypeResolverManagerTypes.StackedTimeSeriesData)
                    .build()))
        .put(TypeResolverManagerUnifaces.ChangeSet,
            getResultTypeResolver(ImmutableMap.<Class, String>builder()
                                      .put(QLUserChangeSet.class, TypeResolverManagerTypes.UserChangeSet)
                                      .put(QLGitChangeSet.class, TypeResolverManagerTypes.GitChangeSet)
                                      .put(QLApiKeyChangeSet.class, TypeResolverManagerTypes.ApiKeyChangeSet)
                                      .put(QLGenericChangeSet.class, TypeResolverManagerTypes.GenericChangeSet)
                                      .build()))
        .put(TypeResolverManagerUnifaces.LinkedSSOSetting,
            getResultTypeResolver(ImmutableMap.<Class, String>builder()
                                      .put(QLLDAPSettings.class, TypeResolverManagerTypes.ldapSettings)
                                      .put(QLSAMLSettings.class, TypeResolverManagerTypes.samlSettings)
                                      .build()))
        .put(TypeResolverManagerUnifaces.Secret,
            getResultTypeResolver(ImmutableMap.<Class, String>builder()
                                      .put(QLEncryptedText.class, TypeResolverManagerTypes.encryptedText)
                                      .put(QLWinRMCredential.class, TypeResolverManagerTypes.winRMCredential)
                                      .build()))
        .build();
  }

  private TypeResolver getResultTypeResolver(Map<Class, String> types) {
    return env -> {
      Object javaObject = env.getObject();
      final Set<Entry<Class, String>> entries = types.entrySet();

      for (Entry<Class, String> entry : types.entrySet()) {
        if (entry.getKey().isAssignableFrom(javaObject.getClass())) {
          return env.getSchema().getObjectType(entry.getValue());
        }
      }

      return null;
    };
  }
}
