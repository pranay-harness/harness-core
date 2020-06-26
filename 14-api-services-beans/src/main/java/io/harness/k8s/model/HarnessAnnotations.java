package io.harness.k8s.model;

public interface HarnessAnnotations {
  String directApply = "harness.io/direct-apply";
  String skipVersioning = "harness.io/skip-versioning";
  String primaryService = "harness.io/primary-service";
  String stageService = "harness.io/stage-service";
  String managed = "harness.io/managed";
  String managedWorkload = "harness.io/managed-workload";
}
