/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.k8s;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class CiK8sTaskResponse {
  @NonNull String podName;
  @NonNull PodStatus podStatus;
  List<String> podStatusLogs;
}
