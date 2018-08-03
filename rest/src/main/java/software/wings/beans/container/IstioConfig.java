package software.wings.beans.container;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class IstioConfig {
  private List<String> gateways;
}
