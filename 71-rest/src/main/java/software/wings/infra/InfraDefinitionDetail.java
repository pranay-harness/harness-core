package software.wings.infra;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.InfrastructureMapping;

import java.util.List;

@Data
@Builder
public class InfraDefinitionDetail {
  private InfrastructureDefinition infrastructureDefinition;
  private List<InfrastructureMapping> derivedInfraMappings;
  private int countDerivedInfraMappings;
}
