package software.wings.service.intfc;

import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;

import java.util.List;
import java.util.Optional;

/**
 * Created by rsingh on 1/9/18.
 */
public interface LearningEngineService {
  void initializeServiceSecretKeys();

  String getServiceSecretKey(ServiceType serviceType);

  Optional<LearningEngineAnalysisTask> getLatestTaskForCvConfigIds(List<String> cvConfigIds);

  boolean checkIfAnalysisHasData(String cvConfigId, MLAnalysisType mlAnalysisType, long minute);
}
