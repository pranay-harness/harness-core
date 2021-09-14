/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.kubectl;

public enum Option {
  namespace,
  filename,
  output,
  toRevision {
    @Override
    public String toString() {
      return "to-revision";
    }
  },
  replicas,
  selector,
  allNamespaces {
    @Override
    public String toString() {
      return "all-namespaces";
    }
  }
}
