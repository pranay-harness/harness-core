/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import io.harness.logging.Misc;

import com.openpojo.reflection.PojoClass;

/**
 * Created by peeyushaggarwal on 5/18/16.
 */
public class ToStringTester extends BaseTester {
  /* (non-Javadoc)
   * @see com.openpojo.validation.test.Tester#run(com.openpojo.reflection.PojoClass)
   */
  @Override
  public void run(PojoClass pojoClass) {
    if (overridesMethod(pojoClass.getClazz(), "toString")) {
      Misc.ignoreException(() -> pojoClass.getClazz().newInstance().toString());
    }
  }
}
