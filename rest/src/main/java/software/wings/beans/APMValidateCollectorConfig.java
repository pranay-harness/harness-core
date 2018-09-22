package software.wings.beans;

import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Data;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.states.APMVerificationState.Method;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class APMValidateCollectorConfig {
  private String baseUrl;
  private String url;
  private String body;
  private Method collectionMethod;
  private Map<String, String> headers;
  private Map<String, String> options;
  List<EncryptedDataDetail> encryptedDataDetails;

  /**
   * Override the getter to make sure we encode backticks always.
   * @return
   */
  public String getUrl() {
    try {
      return url.replaceAll("`", URLEncoder.encode("`", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new WingsException("Unsupported encoding exception while encoding backticks in " + url);
    }
  }
}
