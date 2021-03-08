package io.harness.yaml.schema.helper;

import io.harness.yaml.schema.beans.SupportedPossibleFieldTypes;

import java.util.Comparator;

public class SupportedPossibleFieldTypesComparator implements Comparator<SupportedPossibleFieldTypes> {
  @Override
  public int compare(SupportedPossibleFieldTypes o1, SupportedPossibleFieldTypes o2) {
    return o1.ordinal() - o2.ordinal();
  }
}
