/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc;

import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;

import java.util.List;
import java.util.Optional;

/**
 * Created by rsingh on 1/9/18.
 */
public interface VerificationService {
  Optional<LearningEngineAnalysisTask> getLatestTaskForCvConfigIds(List<String> cvConfigIds);

  boolean checkIfAnalysisHasData(String cvConfigId, MLAnalysisType mlAnalysisType, long minute);
}
