/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
