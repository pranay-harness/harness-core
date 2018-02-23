package software.wings.service.impl.analysis;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public interface CVService {
  void saveCVExecutionMetaData(CVExecutionMetaData cvExecutionMetaData);
  Map<Long, TreeMap<String, Map<String, Map<String, Map<String, List<CVExecutionMetaData>>>>>> getCVExecutionMetaData(
      String accountId, long beginEpochTs, long endEpochTs) throws ParseException;
}
