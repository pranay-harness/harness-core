package io.harness.serializer.morphia;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.entities.CustomActivity;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.entities.InfrastructureActivity;
import io.harness.cvng.activity.entities.KubernetesActivity;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.alert.entities.AlertRule;
import io.harness.cvng.alert.entities.AlertRuleAnomaly;
import io.harness.cvng.analysis.entities.CanaryLogAnalysisLearningEngineTask;
import io.harness.cvng.analysis.entities.ClusteredLog;
import io.harness.cvng.analysis.entities.DeploymentLogAnalysis;
import io.harness.cvng.analysis.entities.DeploymentTimeSeriesAnalysis;
import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisLearningEngineTask;
import io.harness.cvng.analysis.entities.LogAnalysisRecord;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.LogClusterLearningEngineTask;
import io.harness.cvng.analysis.entities.ServiceGuardLogAnalysisTask;
import io.harness.cvng.analysis.entities.TestLogAnalysisLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesAnomalousPatterns;
import io.harness.cvng.analysis.entities.TimeSeriesCanaryLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums;
import io.harness.cvng.analysis.entities.TimeSeriesLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesLoadTestLearningEngineTask;
import io.harness.cvng.analysis.entities.TimeSeriesRiskSummary;
import io.harness.cvng.analysis.entities.TimeSeriesShortTermHistory;
import io.harness.cvng.cd10.entities.CD10EnvMapping;
import io.harness.cvng.cd10.entities.CD10Mapping;
import io.harness.cvng.cd10.entities.CD10ServiceMapping;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.entities.DeploymentDataCollectionTask;
import io.harness.cvng.core.entities.HostRecord;
import io.harness.cvng.core.entities.LogCVConfig;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.ServiceGuardDataCollectionTask;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.core.entities.WebhookToken;
import io.harness.cvng.dashboard.entities.Anomaly;
import io.harness.cvng.dashboard.entities.HealthVerificationHeatMap;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.migration.beans.CVNGSchema;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.verificationjob.entities.BlueGreenVerificationJob;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.HealthVerificationJob;
import io.harness.cvng.verificationjob.entities.TestVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class CVNextGenMorphiaRegister implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Activity.class);
    set.add(ActivitySource.class);
    set.add(AnalysisOrchestrator.class);
    set.add(AnalysisStateMachine.class);
    set.add(CVConfig.class);
    set.add(CVNGSchema.class);
    set.add(DataCollectionTask.class);
    set.add(DeletedCVConfig.class);
    set.add(HealthVerificationHeatMap.class);
    set.add(HeatMap.class);
    set.add(LearningEngineTask.class);
    set.add(LogRecord.class);
    set.add(MetricPack.class);
    set.add(TimeSeriesRecord.class);
    set.add(TimeSeriesThreshold.class);
    set.add(TimeSeriesAnomalousPatterns.class);
    set.add(TimeSeriesCumulativeSums.class);
    set.add(TimeSeriesRiskSummary.class);
    set.add(TimeSeriesShortTermHistory.class);
    set.add(VerificationJobInstance.class);
    set.add(VerificationJob.class);
    set.add(VerificationTask.class);
    set.add(CD10EnvMapping.class);
    set.add(ClusteredLog.class);
    set.add(BlueGreenVerificationJob.class);
    set.add(DeploymentDataCollectionTask.class);
    set.add(CustomActivity.class);
    set.add(AppDynamicsCVConfig.class);
    set.add(Anomaly.class);
    set.add(DeploymentLogAnalysis.class);
    set.add(TestVerificationJob.class);
    set.add(DeploymentTimeSeriesAnalysis.class);
    set.add(CD10ServiceMapping.class);
    set.add(StackdriverCVConfig.class);
    set.add(TestLogAnalysisLearningEngineTask.class);
    set.add(WebhookToken.class);
    set.add(TimeSeriesLoadTestLearningEngineTask.class);
    set.add(MetricCVConfig.class);
    set.add(AlertRuleAnomaly.class);
    set.add(HostRecord.class);
    set.add(HealthVerificationJob.class);
    set.add(SplunkCVConfig.class);
    set.add(KubernetesActivity.class);
    set.add(TimeSeriesCanaryLearningEngineTask.class);
    set.add(CanaryLogAnalysisLearningEngineTask.class);
    set.add(LogCVConfig.class);
    set.add(ServiceGuardDataCollectionTask.class);
    set.add(LogAnalysisCluster.class);
    set.add(LogClusterLearningEngineTask.class);
    set.add(InfrastructureActivity.class);
    set.add(LogAnalysisLearningEngineTask.class);
    set.add(CD10Mapping.class);
    set.add(LogAnalysisResult.class);
    set.add(KubernetesActivitySource.class);
    set.add(LogAnalysisRecord.class);
    set.add(AlertRule.class);
    set.add(CanaryVerificationJob.class);
    set.add(DeploymentActivity.class);
    set.add(ServiceGuardLogAnalysisTask.class);
    set.add(TimeSeriesLearningEngineTask.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
