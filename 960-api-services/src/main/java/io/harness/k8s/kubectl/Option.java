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
