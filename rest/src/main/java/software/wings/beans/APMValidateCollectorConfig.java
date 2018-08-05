package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class APMValidateCollectorConfig {
  private String baseUrl;
  private String url;
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
