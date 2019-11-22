package io.harness.queue;

public interface Queue {
  String getName();

  enum VersionType { VERSIONED, UNVERSIONED }
}
