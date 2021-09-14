/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.dto.perspectives;

import io.harness.ccm.views.graphql.QLCEView;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerspectiveData {
  List<QLCEView> sampleViews;
  List<QLCEView> customerViews;
}
