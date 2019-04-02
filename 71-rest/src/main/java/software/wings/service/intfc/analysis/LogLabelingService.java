package software.wings.service.intfc.analysis;

import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogLabel;
import software.wings.service.impl.analysis.LogMLFeedbackRecord;

import java.util.List;
import java.util.Map;

public interface LogLabelingService {
  List<LogDataRecord> getLogRecordsToClassify(String accountId);
  void saveClassifiedLogRecord(LogDataRecord record, List<LogLabel> labels, String accountId, Object params);
  List<LogLabel> getLabels();

  LogMLFeedbackRecord getIgnoreFeedbackToClassify(String accountId, String serviceId);
  Map<String, List<LogMLFeedbackRecord>> getLabeledSamplesForIgnoreFeedback(String accountId, String serviceId);
  boolean saveLabeledIgnoreFeedback(String accountId, LogMLFeedbackRecord feedbackRecord, String label);
}
