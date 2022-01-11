/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.filter.entity;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterType;
import io.harness.ng.core.common.beans.NGTag;

import java.util.List;
import lombok.Data;

@Data
@OwnedBy(DX)
public abstract class FilterProperties {
  List<NGTag> tags;
  FilterType type;
}
