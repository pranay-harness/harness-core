package io.harness.refrences;

import io.harness.annotations.Redesign;

@Redesign
public interface RefObject {
  String getName();

  String getProducerId();

  RefType getRefType();
}
