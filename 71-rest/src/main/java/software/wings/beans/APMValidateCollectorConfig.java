package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.sm.states.APMVerificationState.Method;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class APMValidateCollectorConfig implements ExecutionCapabilityDemander {
  @NonNull private String baseUrl;
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
      return isEmpty(url) ? "" : url.replaceAll("`", URLEncoder.encode("`", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new WingsException("Unsupported encoding exception while encoding backticks in " + url);
    }
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();
    executionCapabilities.addAll(CapabilityHelper.generateKmsHttpCapabilities(encryptedDataDetails));
    executionCapabilities.addAll(Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(baseUrl)));
    return executionCapabilities;
  }
}
