/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.yaml.handler.infraprovisioner;

import static io.harness.annotations.dev.HarnessModule._870_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDP)
@TargetModule(_870_CG_YAML)
public class TestConstants {
  public static String ACCOUNT_ID = "ACCOUNT_ID";

  public static String SETTING_ID = "SETTING_ID";

  public static String APP_ID = "APP_ID";

  public static String SERVICE_ID = "SERVICE_ID";
}
