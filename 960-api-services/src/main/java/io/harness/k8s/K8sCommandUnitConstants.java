package io.harness.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface K8sCommandUnitConstants {
  String FetchFiles = "Fetch Files";
  String Init = "Initialize";
  String Prepare = "Prepare";
  String Apply = "Apply";
  String Scale = "Scale";
  String Delete = "Delete";
  String Rollback = "Rollback";
  String WaitForSteadyState = "Wait for Steady State";
  String WrapUp = "Wrap Up";
  String TrafficSplit = "Traffic Split";
  String SwapServiceSelectors = "Swap Service Selectors";
  String Prune = "Prune";
  String RecreatePrunedResource = "Recreate Pruned Resources";
  String DeleteFailedReleaseResources = "Delete Failed Release Resources";
}
