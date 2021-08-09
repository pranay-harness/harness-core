package io.harness.persistence;

public interface CreatedAtAware extends CreatedAtAccess {
  void setCreatedAt(long createdAt);
}
