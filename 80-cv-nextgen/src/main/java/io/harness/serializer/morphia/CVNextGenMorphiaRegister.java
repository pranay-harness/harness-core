package io.harness.serializer.morphia;

import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.core.entities.DeletedCVConfig;
import io.harness.cvng.core.entities.LogCVConfig;
import io.harness.cvng.core.entities.LogRecord;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.entities.VerificationTask;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.statemachine.entities.AnalysisOrchestrator;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class CVNextGenMorphiaRegister implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(TimeSeriesRecord.class);
    set.add(AnalysisOrchestrator.class);
    set.add(AnalysisStateMachine.class);
    set.add(HeatMap.class);
    set.add(DataCollectionTask.class);
    set.add(CVConfig.class);
    set.add(DeletedCVConfig.class);
    set.add(SplunkCVConfig.class);
    set.add(AppDynamicsCVConfig.class);
    set.add(LogCVConfig.class);
    set.add(MetricCVConfig.class);
    set.add(MetricPack.class);
    set.add(TimeSeriesThreshold.class);
    set.add(LogRecord.class);
    set.add(VerificationJob.class);
    set.add(VerificationTask.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
