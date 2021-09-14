/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.datadog;

import java.util.Map;

public interface DatadogService {
  String getConcatenatedListOfMetricsForValidation(String defaultMetrics, Map<String, String> dockerMetrics,
      Map<String, String> kubernetesMetrics, Map<String, String> ecsMetrics);
}
