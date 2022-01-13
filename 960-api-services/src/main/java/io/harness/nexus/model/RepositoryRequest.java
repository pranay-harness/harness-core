/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.nexus.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
@Builder
@OwnedBy(HarnessTeam.CDC)
public class RepositoryRequest {
  private String action;
  private String method;
  @JsonProperty("data") private List<RequestData> data;
  private String type;
  private int tid;
}
