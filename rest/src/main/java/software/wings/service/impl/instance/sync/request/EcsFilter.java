package software.wings.service.impl.instance.sync.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcsFilter extends ContainerFilter {
  private Set<String> serviceNameSet;
  private String awsComputeProviderId;
  private String region;
}
