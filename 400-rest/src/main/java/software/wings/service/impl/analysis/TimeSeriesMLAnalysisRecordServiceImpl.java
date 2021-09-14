package software.wings.service.impl.analysis;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.MetricAnalysisRecord.MetricAnalysisRecordKeys;
import software.wings.service.intfc.analysis.TimeSeriesMLAnalysisRecordService;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Sort;
@OwnedBy(HarnessTeam.CV)
public class TimeSeriesMLAnalysisRecordServiceImpl implements TimeSeriesMLAnalysisRecordService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public TimeSeriesMLAnalysisRecord getLastAnalysisRecord(String stateExecutionId) {
    return wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class, excludeAuthority)
        .filter(MetricAnalysisRecordKeys.stateExecutionId, stateExecutionId)
        .order(Sort.descending(MetricAnalysisRecordKeys.analysisMinute))
        .get();
  }

  @Override
  public TimeSeriesMLAnalysisRecord getAnalysisRecordForMinute(String stateExecutionId, Integer analysisMinute) {
    return wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
        .filter(MetricAnalysisRecordKeys.stateExecutionId, stateExecutionId)
        .filter(MetricAnalysisRecordKeys.analysisMinute, analysisMinute)
        .get();
  }
}
