package io.harness.fieldrecaster;

import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;
import io.harness.core.Recaster;

public class SimpleValueFieldRecaster implements FieldRecaster {
  @Override
  public void fromMap(Recaster recaster, RecasterMap recasterMap, CastedField cf, Object entity) {
    recaster.getTransformer().putToEntity(entity, cf, recasterMap);
  }

  @Override
  public void toMap(Recaster recaster, Object entity, CastedField cf, RecasterMap recasterMap) {
    recaster.getTransformer().putToMap(entity, cf, recasterMap);
  }
}
