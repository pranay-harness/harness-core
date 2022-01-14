package io.harness.k8s.kubectl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
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
  },
  dryRunClient {
    @Override
    public String toString() {
      return "dry-run=client";
    }
  }
}
