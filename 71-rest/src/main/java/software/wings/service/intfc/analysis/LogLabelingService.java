package software.wings.service.intfc.analysis;

import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogLabel;

import java.util.List;

public interface LogLabelingService {
  List<LogDataRecord> getLogRecordsToClassify(String accountId);
  void saveClassifiedLogRecord(LogDataRecord record, List<LogLabel> labels, String accountId, Object params);
  List<LogLabel> getLabels();
}
