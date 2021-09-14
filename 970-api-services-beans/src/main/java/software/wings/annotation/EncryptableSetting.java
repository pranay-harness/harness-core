/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.annotation;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.Encryptable;

import software.wings.settings.SettingVariableTypes;

import com.github.reinert.jjschema.SchemaIgnore;

@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._957_CG_BEANS)
public interface EncryptableSetting extends Encryptable {
  @SchemaIgnore SettingVariableTypes getSettingType();
}
