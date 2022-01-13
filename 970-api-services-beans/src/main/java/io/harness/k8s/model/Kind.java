/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
