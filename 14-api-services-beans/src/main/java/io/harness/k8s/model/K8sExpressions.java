package io.harness.k8s.model;

public interface K8sExpressions {
  String primaryServiceNameExpression = "${k8s.primaryServiceName}";
  String stageServiceNameExpression = "${k8s.stageServiceName}";
  String canaryWorkloadExpression = "${k8s.canaryWorkload}";
  String primaryServiceName = "k8s.primaryServiceName";
  String stageServiceName = "k8s.stageServiceName";
  String canaryWorkload = "k8s.canaryWorkload";

  String virtualServiceName = "k8s.virtualServiceName";
  String virtualServiceNameExpression = "${k8s.virtualServiceName}";
  String canaryDestination = "k8s.canaryDestination";
  String canaryDestinationExpression = "${k8s.canaryDestination}";
  String stableDestination = "k8s.stableDestination";
  String stableDestinationExpression = "${k8s.stableDestination}";
}
