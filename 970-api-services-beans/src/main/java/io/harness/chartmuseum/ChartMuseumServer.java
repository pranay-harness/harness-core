/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.chartmuseum;

import lombok.Builder;
import lombok.Data;
import org.zeroturnaround.exec.StartedProcess;

@Data
@Builder
public class ChartMuseumServer {
  StartedProcess startedProcess;
  int port;
}
