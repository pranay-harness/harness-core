/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.beans.recommendation;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResourceId {
  public static final ResourceId NOT_FOUND = ResourceId.builder().build();

  String accountId;
  String clusterId;
  String namespace;
  String name;
  String kind;
}
