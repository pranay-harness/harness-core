package io.harness.cdng.infra.beans;

import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
public class K8sDirectInfraMapping implements InfraMapping {
  @Id private String uuid;
  private String accountId;
  private String k8sConnector;
  private String namespace;
  private String serviceIdentifier;
}
