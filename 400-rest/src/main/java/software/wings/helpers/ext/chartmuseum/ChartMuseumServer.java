package software.wings.helpers.ext.chartmuseum;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;
import org.zeroturnaround.exec.StartedProcess;

@Data
@Builder
@TargetModule(Module._970_API_SERVICES_BEANS)
public class ChartMuseumServer {
  StartedProcess startedProcess;
  int port;
}
