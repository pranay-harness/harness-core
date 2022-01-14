package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.kubernetes.client.custom.Quantity;
import java.util.Map;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ResourceRequirements {
  private Map<String, Quantity> limits;
  private Map<String, Quantity> requests;
}
