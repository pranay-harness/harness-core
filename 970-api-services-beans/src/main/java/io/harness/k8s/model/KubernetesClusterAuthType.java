/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.model;

public enum KubernetesClusterAuthType { NONE, OIDC, SERVICE_ACCOUNT, CLIENT_KEY_CERT, USER_PASSWORD }
