package io.harness.cache;

public interface Ordinal {
  // Returns a order of the context in which the object was prepared.
  long contextOrder();
}
