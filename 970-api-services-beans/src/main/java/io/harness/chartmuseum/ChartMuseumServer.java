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
