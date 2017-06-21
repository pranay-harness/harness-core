package software.wings.service.impl.splunk;

import static software.wings.dl.PageRequest.Builder.aPageRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.splunk.SplunkService;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rsingh on 4/17/17.
 */
@ValidateOnExecution
public class SplunkServiceImpl implements SplunkService {
  private static final Logger logger = LoggerFactory.getLogger(SplunkServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;

  @Override
  public Boolean saveLogData(String appId, List<SplunkLogElement> logData) throws IOException {
    logger.debug("inserting " + logData.size() + " pieces of splunk log data");
    final List<SplunkLogDataRecord> logDataRecords = SplunkLogDataRecord.generateDataRecords(appId, logData);
    wingsPersistence.saveIgnoringDuplicateKeys(logDataRecords);
    logger.debug("inserted " + logDataRecords.size() + " SplunkLogDataRecord to persistence layer.");
    return true;
  }

  @Override
  public List<SplunkLogDataRecord> getSplunkLogData(SplunkLogRequest logRequest) {
    PageRequest.Builder amdrRequestBuilder = aPageRequest()
                                                 .addFilter("applicationId", Operator.EQ, logRequest.getApplicationId())
                                                 .addFilter("timeStamp", Operator.GT, logRequest.getStartTime() - 1)
                                                 .addFilter("timeStamp", Operator.LT, logRequest.getEndTime() - 1)
                                                 .addFilter("host", Operator.IN, logRequest.getNodes().toArray())
                                                 .addOrder("timeStamp", OrderType.ASC)
                                                 .withLimit(PageRequest.UNLIMITED);
    return wingsPersistence.query(SplunkLogDataRecord.class, amdrRequestBuilder.build());
  }
}
