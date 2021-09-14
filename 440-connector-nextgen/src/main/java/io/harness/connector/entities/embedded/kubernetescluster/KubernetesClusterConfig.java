/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.entities.embedded.kubernetescluster;

import io.harness.connector.entities.Connector;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "KubernetesClusterConfigKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterConfig")
public class KubernetesClusterConfig extends Connector {
  KubernetesCredentialType credentialType;
  KubernetesCredential credential;
}
