package software.wings.service.intfc;

import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.impl.ThirdPartyApiCallLog;

import java.util.List;

/**
 * Created by rsingh on 6/14/18.
 */
public interface ThirdPartyApiService {
  boolean saveApiCallLog(List<ThirdPartyApiCallLog> externalApiCallLogs);

  PageResponse<ThirdPartyApiCallLog> list(PageRequest<ThirdPartyApiCallLog> pageRequest);
}
