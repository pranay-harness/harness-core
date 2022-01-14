package io.harness.yaml.schema.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.schema.beans.FieldSubtypeData;

import java.util.Comparator;

@OwnedBy(DX)
public class FieldSubTypeComparator implements Comparator<FieldSubtypeData> {
  @Override
  public int compare(FieldSubtypeData o1, FieldSubtypeData o2) {
    return o1.getFieldName().compareTo(o2.getFieldName());
  }
}
