/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class HarnessUserThreadLocal {
  public static final ThreadLocal<HarnessUserAccountActions> values = new ThreadLocal<>();

  public static void set(HarnessUserAccountActions harnessUserAccountActions) {
    values.set(harnessUserAccountActions);
  }

  public static void unset() {
    values.remove();
  }

  public static HarnessUserAccountActions get() {
    return values.get();
  }
}
