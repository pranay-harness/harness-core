package software.wings.api.pcf;

import io.harness.data.SweepingOutput;
import lombok.Builder;
import lombok.Value;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;

@Value
@Builder
public class SwapRouteRollbackSweepingOutputPcf implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "pcfSwapRouteRollbackSweepingOutput";
  private String uuid;
  private String name;
  private PcfRouteUpdateRequestConfigData pcfRouteUpdateRequestConfigData;
}
