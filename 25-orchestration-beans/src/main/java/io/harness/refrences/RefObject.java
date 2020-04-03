package io.harness.refrences;

import io.harness.annotations.Redesign;

@Redesign
public interface RefObject {
  enum RefType { CONTEXT, SWEEPING_OUTPUT }

  String getName();
  String getProducerId();
  RefType getRefType();
}
