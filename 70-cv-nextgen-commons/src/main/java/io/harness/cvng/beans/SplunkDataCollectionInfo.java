package io.harness.cvng.beans;

import io.harness.cvng.models.VerificationType;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class SplunkDataCollectionInfo extends DataCollectionInfo<SplunkConnectorDTO> {
  private String query;
  private String serviceInstanceIdentifier;
  @Override
  public VerificationType getVerificationType() {
    return VerificationType.LOG;
  }

  @Override
  public Map<String, Object> getDslEnvVariables() {
    Map<String, Object> map = new HashMap<>();
    map.put("query", query);
    map.put("serviceInstanceIdentifier", "$." + serviceInstanceIdentifier);
    // TODO: setting max to 10000 now. We need to find a generic way to throw exception
    // in case of too many logs.
    map.put("maxCount", 10000);
    return map;
  }

  @Override
  public String getBaseUrl(SplunkConnectorDTO splunkConnectorDTO) {
    return splunkConnectorDTO.getSplunkUrl();
  }

  @Override
  public Map<String, String> collectionHeaders(SplunkConnectorDTO splunkConnectorDTO) {
    String decryptedPassword = new String(splunkConnectorDTO.getPasswordRef().getDecryptedValue());
    String usernameColonPassword = splunkConnectorDTO.getUsername().concat(":").concat(decryptedPassword);
    String auth =
        "Basic " + Base64.getEncoder().encodeToString(usernameColonPassword.getBytes(Charset.forName("UTF-8")));
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", auth);
    return headers;
  }

  @Override
  public Map<String, String> collectionParams(SplunkConnectorDTO splunkConnectorDTO) {
    return Collections.emptyMap();
  }
}
