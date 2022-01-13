/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
