package software.wings.helpers.ext.chartmuseum;

import lombok.Builder;
import lombok.Data;
import org.zeroturnaround.exec.StartedProcess;

@Data
@Builder
public class ChartMuseumServer {
  StartedProcess startedProcess;
  int port;
}
