package io.harness.argo.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppSyncOptions {
  String revision;
  boolean prune;
  boolean dryRun;
  Resources resources;
  Strategy strategy;
  Options options;

  private class Options {}
  private class Resources {}
  private class Strategy {}

  public static AppSyncOptions DefaultSyncOptions() {
    return AppSyncOptions.builder().revision("HEAD").dryRun(false).prune(false).build();
  }
}
