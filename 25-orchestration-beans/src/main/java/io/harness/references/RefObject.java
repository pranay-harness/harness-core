package io.harness.references;

import io.harness.annotations.Redesign;

@Redesign
public interface RefObject {
  String getName();

  String getProducerId();

  RefType getRefType();
}
