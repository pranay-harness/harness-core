/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model;

public enum Kind {
  Namespace,
  ResourceQuota,
  LimitRange,
  PodSecurityPolicy,
  Secret,
  ConfigMap,
  StorageClass,
  PersistentVolume,
  PersistentVolumeClaim,
  ServiceAccount,
  CustomResourceDefinition,
  ClusterRole,
  ClusterRoleBinding,
  Role,
  RoleBinding,
  Service,
  DaemonSet,
  Pod,
  ReplicationController,
  ReplicaSet,
  Deployment,
  DeploymentConfig,
  StatefulSet,
  Job,
  CronJob,
  Ingress,
  APIService,
  VirtualService,
  DestinationRule
}
