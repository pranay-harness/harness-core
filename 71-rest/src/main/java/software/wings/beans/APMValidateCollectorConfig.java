package software.wings.beans;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.sm.states.APMVerificationState.Method;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class APMValidateCollectorConfig implements ExecutionCapabilityDemander {
  @NonNull private String baseUrl;
  @NonNull private String url;
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

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getUrl()));
  }
}
