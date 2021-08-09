package io.harness.k8s.model;

import io.harness.data.validator.Trimmed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IstioDestinationWeight {
  @Trimmed @NotEmpty private String destination;
  @Trimmed @NotEmpty private String weight;
}
