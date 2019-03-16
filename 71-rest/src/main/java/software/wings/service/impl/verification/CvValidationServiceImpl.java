package software.wings.service.impl.verification;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.service.intfc.elk.ElkAnalysisService;

/**
 * Created by Pranjal on 03/14/2019
 */
@Singleton
public class CvValidationServiceImpl implements CvValidationService {
  @Inject private ElkAnalysisService elkAnalysisService;

  @Override
  public Boolean validateELKQuery(String accountId, String appId, String settingId, String query, String index) {
    return elkAnalysisService.validateQuery(accountId, appId, settingId, query, index, null);
  }
}
