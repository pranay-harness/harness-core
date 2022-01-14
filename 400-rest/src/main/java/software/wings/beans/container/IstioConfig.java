package software.wings.beans.container;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class IstioConfig {
  private List<String> gateways;
  private List<String> hosts;
}
