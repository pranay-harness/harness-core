/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.analysis;

import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.LabeledLogRecord;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogLabel;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public interface LogLabelingService {
  List<LogDataRecord> getLogRecordsToClassify(String accountId);
  void saveClassifiedLogRecord(LogDataRecord record, List<LogLabel> labels, String accountId, Object params);
  List<LogLabel> getLabels();

  List<CVFeedbackRecord> getCVFeedbackToClassify(String accountId, String serviceId);
  Map<String, List<CVFeedbackRecord>> getLabeledSamplesForIgnoreFeedback(
      String accountId, String serviceId, String envId);
  List<CVFeedbackRecord> getCVFeedbackToClassify(String accountId);
  Map<String, List<CVFeedbackRecord>> getLabeledSamplesForIgnoreFeedback(String accountId);
  boolean saveLabeledIgnoreFeedback(String accountId, CVFeedbackRecord feedbackRecord, String label);
  boolean saveLabeledIgnoreFeedback(String accountId, Map<String, List<CVFeedbackRecord>> feedbackRecordMap);
  Map<Pair<String, String>, Integer> getAccountsWithFeedback();
  Map<Pair<String, String>, Integer> getServicesWithFeedbackForAccount(String accountId);
  Map<String, List<String>> getSampleLabeledRecords(String serviceId, String envId);
  List<LogDataRecord> getL2RecordsToClassify(String serviceId, String envId);
  boolean saveLabeledL2AndFeedback(List<LabeledLogRecord> labeledLogRecords);
}
