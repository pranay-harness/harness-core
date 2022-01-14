package io.harness.beans.script;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScriptInfo {
  // TODO Improve it by taking proper script type and input format
  private String scriptString;

  private boolean fromTemplate;
}
