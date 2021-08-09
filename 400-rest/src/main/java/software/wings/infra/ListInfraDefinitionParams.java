package software.wings.infra;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListInfraDefinitionParams {
  private List<String> deploymentTypeFromMetaData;
  private List<String> serviceIds;
}
