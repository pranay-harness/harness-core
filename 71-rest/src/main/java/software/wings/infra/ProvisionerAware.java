package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;
import java.util.Set;

public interface ProvisionerAware {
  Map<String, String> getExpressions();
  void setExpressions(Map<String, String> expressions);
  @JsonIgnore Set<String> getSupportedExpressions();
  void applyExpressions(Map<String, Object> resolvedExpressions);
}
