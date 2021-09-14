/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.writer.constants;

public class K8sCCMConstants {
  public static final String AWS_LIFECYCLE_KEY = "lifecycle";
  public static final String AWS_CAPACITY_TYPE_KEY = "capacityType";
  public static final String AZURE_LIFECYCLE_KEY = "kubernetes.azure.com/scalesetpriority";
  public static final String RELEASE_NAME = "harness.io/release-name";
  public static final String HELM_RELEASE_NAME = "release";
  public static final String K8SV1_RELEASE_NAME = "harness.io/service-infra-id";
  public static final String OPERATING_SYSTEM = "beta.kubernetes.io/os";
  public static final String PREEMPTIBLE_KEY = "preemptible";
  public static final String REGION = "failure-domain.beta.kubernetes.io/region";
  public static final String ZONE = "failure-domain.beta.kubernetes.io/zone";
  public static final String INSTANCE_FAMILY = "beta.kubernetes.io/instance-type";
  public static final String GKE_NODE_POOL_KEY = "cloud.google.com/gke-nodepool";
  public static final String AKS_NODE_POOL_KEY = "agentpool";
  public static final String EKS_NODE_POOL_KEY = "alpha.eksctl.io/nodegroup-name";
  public static final String COMPUTE_TYPE = "eks.amazonaws.com/compute-type";
  public static final String UNALLOCATED = "Unallocated";
  public static final String DEFAULT_DEPLOYMENT_TYPE = "Pod";
  public static final String AWS_FARGATE_COMPUTE_TYPE = "fargate";

  private K8sCCMConstants() {}
}
