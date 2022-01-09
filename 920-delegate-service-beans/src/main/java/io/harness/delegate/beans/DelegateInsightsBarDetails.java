/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.DEL)
@FieldNameConstants(innerTypeName = "DelegateInsightsBarDetailsKeys")
@Data
@Builder
public class DelegateInsightsBarDetails {
  private long timeStamp;
  @Default private List<Pair<DelegateInsightsType, Long>> counts = new ArrayList<>();
}
