/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.fieldrecaster;

import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;
import io.harness.core.Recaster;

public interface FieldRecaster {
  void fromMap(Recaster recaster, RecasterMap recasterMap, CastedField cf, Object entity);

  void toMap(Recaster recaster, Object entity, CastedField cf, RecasterMap recasterMap);
}
