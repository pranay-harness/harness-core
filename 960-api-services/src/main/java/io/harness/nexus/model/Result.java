/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.nexus.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class Result {
  private boolean success;
  private List<ResponseData> data = new ArrayList<>();
}
