package io.harness.cvng.metrics.services.impl;

import io.harness.cvng.analysis.entities.LearningEngineTask;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.core.entities.DataCollectionTask;
import io.harness.cvng.metrics.beans.AccountMetricContext;
import io.harness.cvng.metrics.beans.DataCollectionTaskMetricContext;
import io.harness.cvng.statemachine.entities.AnalysisStateMachine;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.metrics.AutoMetricContext;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class MetricContextBuilder {
  private static Map<Class<?>, ObjectContextBuilder<?>> OBJECT_CONTEXT_BUILDER_MAP = new HashMap<>();
  static {
    addToObjContextMap(DataCollectionTask.class,
        dataCollectionTask
        -> new DataCollectionTaskMetricContext(dataCollectionTask.getAccountId(),
            dataCollectionTask.getType().name().toLowerCase(),
            dataCollectionTask.getDataCollectionInfo().getClass().getSimpleName().replace("DataCollectionInfo", ""),
            dataCollectionTask.getRetryCount()));
    addToObjContextMap(CVNGStepTask.class, cvngStepTask -> new AccountMetricContext(cvngStepTask.getAccountId()));
    addToObjContextMap(VerificationJobInstance.class,
        verificationJobInstance -> new AccountMetricContext(verificationJobInstance.getAccountId()));
    addToObjContextMap(
        LearningEngineTask.class, learningEngineTask -> new AccountMetricContext(learningEngineTask.getAccountId()));
    addToObjContextMap(AnalysisStateMachine.class,
        analysisStateMachine -> new AccountMetricContext(analysisStateMachine.getAccountId()));
  }

  private static <T> void addToObjContextMap(Class<T> clazz, ObjectContextBuilder<T> objectContextBuilder) {
    OBJECT_CONTEXT_BUILDER_MAP.put(clazz, objectContextBuilder);
  }

  public <T> AutoMetricContext getContext(T obj, Class<T> clazz) {
    Preconditions.checkState(OBJECT_CONTEXT_BUILDER_MAP.containsKey(clazz),
        "Object context builder is not defined for class: %s", obj.getClass());
    ObjectContextBuilder<T> objectContextBuilder = (ObjectContextBuilder<T>) OBJECT_CONTEXT_BUILDER_MAP.get(clazz);
    return objectContextBuilder.create(obj);
  }

  private interface ObjectContextBuilder<T> {
    AutoMetricContext create(T obj);
  }
}
