package software.wings.beans;

import com.google.common.collect.ImmutableSet;

import io.harness.event.model.QueableEvent;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.DeploymentEvent;
import software.wings.api.DeploymentSummary;
import software.wings.api.InstanceChangeEvent;
import software.wings.api.KmsTransitionEvent;
import software.wings.audit.AuditHeader;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.artifact.AcrArtifactStream;
import software.wings.beans.artifact.AmazonS3ArtifactStream;
import software.wings.beans.artifact.AmiArtifactStream;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactoryArtifactStream;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.beans.artifact.GcsArtifactStream;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.artifact.SftpArtifactStream;
import software.wings.beans.artifact.SmbArtifactStream;
import software.wings.beans.baseline.WorkflowExecutionBaseline;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.config.ArtifactSourceable;
import software.wings.beans.container.ContainerTask;
import software.wings.beans.container.EcsContainerTask;
import software.wings.beans.container.EcsServiceSpecification;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.KubernetesContainerTask;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.beans.container.UserDataSpecification;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.TerraformfConfig;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.ManualSyncJob;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.beans.infrastructure.instance.stats.InstanceStatsSnapshot;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SamlSettings;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateFolder;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.VersionedTemplate;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.collect.CollectEvent;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.prune.PruneEvent;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.DelayEvent;
import software.wings.service.impl.ExecutionEvent;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.service.impl.analysis.ExperimentalMetricAnalysisRecord;
import software.wings.service.impl.analysis.LabeledLogRecord;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;
import software.wings.service.impl.analysis.MetricAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup;
import software.wings.service.impl.analysis.TimeSeriesMetricTemplates;
import software.wings.service.impl.analysis.TimeSeriesRiskSummary;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.LearningEngineExperimentalAnalysisTask;
import software.wings.service.impl.newrelic.MLExperiments;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.security.EncryptionConfig;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateMachine;
import software.wings.verification.CVConfiguration;
import software.wings.verification.apm.APMCVServiceConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.log.AbstractLogsCVConfiguration;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;
import software.wings.yaml.YamlHistory;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.gitSync.GitSyncWebhook;
import software.wings.yaml.gitSync.YamlChangeSet;
import software.wings.yaml.gitSync.YamlGitConfig;

import java.security.Principal;
import java.util.Set;

public class ManagerMorphiaClasses {
  // TODO: this is temporarily listing all the classes in the manager.
  //       Step by step this should be split in different dedicated sections

  public static final Set<Class> classes = ImmutableSet.<Class>of(FeatureFlag.class, InfrastructureProvisioner.class,
      ContainerTask.class, DeploymentTrigger.class, AmazonS3ArtifactStream.class, HelmChartSpecification.class,
      ManagerConfiguration.class, DelegateProfile.class, TerraformfConfig.class, DelegateScope.class,
      SystemCatalog.class, SamlSettings.class, ArtifactoryArtifactStream.class, ContainerDeploymentInfo.class,
      Template.class, Instance.class, NexusArtifactStream.class, ExternalServiceAuthToken.class,
      CodeDeployInfrastructureMapping.class, Idempotent.class, GcrArtifactStream.class, EntityVersionCollection.class,
      ManualSyncJob.class, GcsArtifactStream.class, PhysicalInfrastructureMappingBase.class,
      PhysicalInfrastructureMapping.class, ResourceConstraintInstance.class, BambooArtifactStream.class,
      GcpKubernetesInfrastructureMapping.class, TemplateVersion.class, UserInvite.class, ApprovalNotification.class,
      AzureKubernetesInfrastructureMapping.class, TriggerExecution.class, GitCommit.class, SmbArtifactStream.class,
      AwsLambdaInfraStructureMapping.class, Schema.class, VaultConfig.class, User.class, AnalysisContext.class,
      ApiKeyEntry.class, PipelineExecution.class, HarnessUserGroup.class, FailureNotification.class, Service.class,
      Whitelist.class, DeploymentPreference.class, EntityVersion.class, BaseFile.class,
      CloudFormationInfrastructureProvisioner.class, BarrierInstance.class, ResourceConstraint.class,
      DirectKubernetesInfrastructureMapping.class, DelegateTask.class, Alert.class, EcrArtifactStream.class,
      ServiceSecretKey.class, ArtifactStream.class, ActionableNotification.class, AppContainer.class,
      EcsContainerTask.class, SweepingOutput.class, PhysicalInfrastructureMappingWinRm.class, CacheEntity.class,
      Preference.class, PcfServiceSpecification.class, ResourceConstraintNotification.class,
      EcsInfrastructureMapping.class, LdapSettings.class, ServiceVariable.class, KmsConfig.class,
      AmiArtifactStream.class, JenkinsArtifactStream.class, AzureInfrastructureMapping.class,
      ContainerInfrastructureMapping.class, Role.class, Command.class, SettingAttribute.class,
      WorkflowExecutionBaseline.class, DockerArtifactStream.class, Workflow.class, PcfInfrastructureMapping.class,
      SSOSettings.class, Notification.class, Log.class, DeploymentSpecification.class,
      AwsAmiInfrastructureMapping.class, SyncStatus.class, Application.class, LambdaSpecification.class, License.class,
      Environment.class, ServiceCommand.class, AuthToken.class, EcsServiceSpecification.class,
      EmailVerificationToken.class, ConfigFile.class, Base.class, SftpArtifactStream.class, NotificationGroup.class,
      ServiceInstance.class, Activity.class, Account.class, Host.class, UserDataSpecification.class,
      KubernetesContainerTask.class, TemplateGallery.class, ServiceTemplate.class, AcrArtifactStream.class,
      Pipeline.class, UserGroup.class, TemplateFolder.class, InstanceStatsSnapshot.class, Trigger.class,
      AwsInfrastructureMapping.class, DelegateConnection.class, ApplicationManifest.class, Delegate.class,
      InformationNotification.class, NotificationBatch.class, WorkflowExecution.class, VersionedTemplate.class,
      Artifact.class, TerraformInfrastructureProvisioner.class, ArtifactSourceable.class, InfrastructureMapping.class,
      LogMLFeedbackRecord.class, MLExperiments.class, ThirdPartyApiCallLog.class, NewRelicMetricDataRecord.class,
      GitSyncError.class, KmsTransitionEvent.class, TimeSeriesMetricGroup.class,
      ContinuousVerificationExecutionMetaData.class, CommandUnit.class, CloudWatchCVServiceConfiguration.class,
      DelayEvent.class, ExperimentalLogMLAnalysisRecord.class, NewRelicMetricAnalysisRecord.class, CollectEvent.class,
      SecretChangeLog.class, TimeSeriesMLScores.class, AppDynamicsCVServiceConfiguration.class,
      EncryptableSetting.class, DeploymentSummary.class, LearningEngineExperimentalAnalysisTask.class,
      StateMachine.class, CVConfiguration.class, DelegateConnectionResult.class, DeploymentEvent.class,
      AuditHeader.class, TimeSeriesMetricTemplates.class, SecretUsageLog.class, DynaTraceCVServiceConfiguration.class,
      LogDataRecord.class, InstanceChangeEvent.class, YamlChangeSet.class, YamlHistory.class,
      LearningEngineAnalysisTask.class, ExperimentalMetricAnalysisRecord.class, Comparable.class,
      LogMLAnalysisRecord.class, TimeSeriesMLAnalysisRecord.class, StateExecutionInstance.class,
      DatadogCVServiceConfiguration.class, MetricAnalysisRecord.class, YamlVersion.class, EncryptionConfig.class,
      Principal.class, PrometheusCVServiceConfiguration.class, EncryptedData.class,
      NewRelicCVServiceConfiguration.class, ExecutionInterrupt.class, APMCVServiceConfiguration.class, EmailData.class,
      YamlGitConfig.class, TimeSeriesMLTransactionThresholds.class, TimeSeriesRiskSummary.class, GitSyncWebhook.class,
      ExecutionEvent.class, ManifestFile.class, GcsFileMetadata.class, QueableEvent.class, PruneEvent.class,
      AbstractLogsCVConfiguration.class, ElkCVConfiguration.class, Permit.class, LabeledLogRecord.class,
      AlertNotificationRule.class, CustomArtifactStream.class);
}
