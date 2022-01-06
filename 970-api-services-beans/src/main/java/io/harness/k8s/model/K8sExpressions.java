/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
