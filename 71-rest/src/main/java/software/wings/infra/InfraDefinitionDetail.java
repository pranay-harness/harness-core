package software.wings.infra;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InfraDefinitionDetail {
  private InfrastructureDefinition infrastructureDefinition;
  private List<InfraMappingDetail> derivedInfraMappingDetailList;
  private int countDerivedInfraMappings;
}
