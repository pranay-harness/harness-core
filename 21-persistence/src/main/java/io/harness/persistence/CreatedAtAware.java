package io.harness.persistence;

public interface CreatedAtAware extends CreatedAtAccess {
  String CREATED_AT_KEY = "createdAt";

  void setCreatedAt(long createdAt);
}
