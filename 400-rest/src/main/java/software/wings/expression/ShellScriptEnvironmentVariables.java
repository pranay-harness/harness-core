/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.expression;

import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShellScriptEnvironmentVariables {
  @Getter private final Map<String, String> outputVars;
  @Getter private final Map<String, String> secretOutputVars;

  public ShellScriptEnvironmentVariables(Map<String, String> outputVars, Map<String, String> secretOutputVars) {
    this.outputVars = outputVars;
    this.secretOutputVars = secretOutputVars;
  }

  public Object get(Object key) {
    if (outputVars.containsKey(key)) {
      return outputVars.get(key);
    }
    if (secretOutputVars.containsKey(key)) {
      String value = secretOutputVars.get(key);
      String keyString = (String) key;
      return "${sweepingOutputSecrets.obtain(\"" + keyString + "\",\"" + value + "\")}";
    }
    return null;
  }
}
