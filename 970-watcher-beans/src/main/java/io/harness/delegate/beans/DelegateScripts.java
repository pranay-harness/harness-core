/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
