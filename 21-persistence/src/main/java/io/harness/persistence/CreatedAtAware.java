package io.harness.persistence;

public interface CreatedAtAware {
  String CREATED_AT_KEY = "createdAt";

  long getCreatedAt();
  void setCreatedAt(long createdAt);
}
