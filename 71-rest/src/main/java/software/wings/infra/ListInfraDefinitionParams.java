package software.wings.infra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListInfraDefinitionParams {
  private List<String> deploymentTypeFromMetaData;
  private List<String> serviceIds;
}
