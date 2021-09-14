/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EcsSetupContextVariableHolder {
  private Map<String, String> serviceVariables;
  private Map<String, String> safeDisplayServiceVariables;
}
