/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.validation.Validator;

import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.provision.terraform.TerraformConfigFilesWrapper")
public class TerraformConfigFilesWrapper {
  @NotNull StoreConfigWrapper store;

  public void validateParams() {
    Validator.notNullCheck("Store cannot be null in Config Files", store);
    store.validateParams();
  }
}
