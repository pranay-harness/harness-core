package software.wings.service.intfc.analysis;

import software.wings.service.impl.analysis.CVFeedbackRecord;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogLabel;

import java.util.List;
import java.util.Map;

public interface LogLabelingService {
  List<LogDataRecord> getLogRecordsToClassify(String accountId);
  void saveClassifiedLogRecord(LogDataRecord record, List<LogLabel> labels, String accountId, Object params);
  List<LogLabel> getLabels();

  CVFeedbackRecord getCVFeedbackToClassify(String accountId, String serviceId, String envId);
  Map<String, List<CVFeedbackRecord>> getLabeledSamplesForIgnoreFeedback(
      String accountId, String serviceId, String envId);
  CVFeedbackRecord getCVFeedbackToClassify(String accountId);
  Map<String, List<CVFeedbackRecord>> getLabeledSamplesForIgnoreFeedback(String accountId);
  boolean saveLabeledIgnoreFeedback(String accountId, CVFeedbackRecord feedbackRecord, String label);
}
