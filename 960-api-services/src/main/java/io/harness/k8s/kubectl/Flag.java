/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
