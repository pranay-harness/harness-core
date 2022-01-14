package software.wings.infra;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InfraDefinitionDetail {
  private InfrastructureDefinition infrastructureDefinition;
  private List<InfraMappingDetail> derivedInfraMappingDetailList;
  private int countDerivedInfraMappings;
}
