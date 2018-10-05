package io.harness.kubectl;

public enum Flag {
  dryrun {
    @Override
    public String toString() {
      return "dry-run";
    }
  },
  record
}
