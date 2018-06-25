package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ThirdPartyApiService;

import java.util.List;

/**
 * Created by rsingh on 6/14/18.
 */
public class ThirdPartyApiServiceImpl implements ThirdPartyApiService {
  private static final Logger logger = LoggerFactory.getLogger(ThirdPartyApiServiceImpl.class);
  @Inject protected WingsPersistence wingsPersistence;
  @Override
  public boolean saveApiCallLog(List<ThirdPartyApiCallLog> apiCallLogs) {
    if (isEmpty(apiCallLogs)) {
      return false;
    }
    wingsPersistence.saveIgnoringDuplicateKeys(apiCallLogs);
    logger.info("Inserted {} data collection records", apiCallLogs.size());
    return true;
  }

  @Override
  public PageResponse<ThirdPartyApiCallLog> list(PageRequest<ThirdPartyApiCallLog> pageRequest) {
    return wingsPersistence.query(ThirdPartyApiCallLog.class, pageRequest);
  }
}
