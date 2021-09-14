/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class DelegateScripts {
  private String version;
  private boolean doUpgrade;
  private String stopScript;
  private String startScript;
  private String delegateScript;
  private String setupProxyScript;

  public String getScriptByName(String fileName) {
    switch (fileName) {
      case "start.sh":
        return getStartScript();
      case "stop.sh":
        return getStopScript();
      case "delegate.sh":
        return getDelegateScript();
      case "setup-proxy.sh":
        return getSetupProxyScript();
      default:
        return null;
    }
  }
}
