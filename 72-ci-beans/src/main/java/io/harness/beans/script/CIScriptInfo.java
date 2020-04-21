package io.harness.beans.script;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

@Data
@Value
@Builder
public class CIScriptInfo {
  // Improve it by taking proper script type and input format
  private String scriptString;

  private boolean fromTemplate;
}
