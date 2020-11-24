package io.harness.k8s.kubectl;

public enum Flag {
  dryrun {
    @Override
    public String toString() {
      return "dry-run";
    }
  },
  export,
  record,
  watch,
  watchOnly {
    @Override
    public String toString() {
      return "watch-only";
    }
  }
}
