/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.entities;

import lombok.Getter;

public enum ViewCustomFunction {
  ONE_OF("ONE_OF", "`ce-qa-274307.BillingReport_zeaak_fls425ieo7olzmug.oneOf`");

  @Getter private final String name;
  @Getter private final String formula;

  ViewCustomFunction(String name, String formula) {
    this.name = name;
    this.formula = formula;
  }
}
