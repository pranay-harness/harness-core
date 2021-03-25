package io.harness.yaml.schema.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.schema.beans.SubtypeClassMap;

import java.util.Comparator;

@OwnedBy(DX)
public class SubtypeClassMapComparator implements Comparator<SubtypeClassMap> {
  @Override
  public int compare(SubtypeClassMap o1, SubtypeClassMap o2) {
    return o1.getSubtypeEnum().compareTo(o2.getSubtypeEnum());
  }
}
