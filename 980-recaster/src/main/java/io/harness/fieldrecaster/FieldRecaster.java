/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.fieldrecaster;

import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;
import io.harness.core.Recaster;

public interface FieldRecaster {
  void fromMap(Recaster recaster, RecasterMap recasterMap, CastedField cf, Object entity);

  void toMap(Recaster recaster, Object entity, CastedField cf, RecasterMap recasterMap);
}
