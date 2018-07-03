package io.harness.cache;

public interface Nominal {
  // Returns a hash of the context in which the object was prepared.
  long contextHash();
}
