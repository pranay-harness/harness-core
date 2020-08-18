package io.harness.cvng.beans.appd;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AppDynamicsApplication {
  String name;
  long id;
}
