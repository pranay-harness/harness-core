package io.harness.yaml.schema.helper;

import io.harness.yaml.schema.beans.SubtypeClassMap;

import java.util.Comparator;

public class SubtypeClassMapComparator implements Comparator<SubtypeClassMap> {
  @Override
  public int compare(SubtypeClassMap o1, SubtypeClassMap o2) {
    return o1.getSubtypeEnum().compareTo(o2.getSubtypeEnum());
  }
}
