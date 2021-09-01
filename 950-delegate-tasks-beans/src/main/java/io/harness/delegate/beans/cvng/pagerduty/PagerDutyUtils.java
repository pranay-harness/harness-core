package io.harness.delegate.beans.cvng.pagerduty;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.pagerduty.PagerDutyConnectorDTO;

import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
@OwnedBy(CV)
public class PagerDutyUtils {
  private static final String PAGER_DUTY_URL = "https://api.pagerduty.com/";

  public static String getBaseUrl() {
    return PAGER_DUTY_URL;
  }

  private static String getAuthorizationHeader(PagerDutyConnectorDTO pagerDutyConnectorDTO) {
    return "Token token=" + new String(pagerDutyConnectorDTO.getApiTokenRef().getDecryptedValue());
  }

  public static Map<String, String> collectionHeaders(PagerDutyConnectorDTO pagerDutyConnectorDTO) {
    Map<String, String> headers = new HashMap<>();
    headers.put("Connection", "keep-alive");
    headers.put("Authorization", getAuthorizationHeader(pagerDutyConnectorDTO));
    return headers;
  }
}
