package io.harness.cdng.infra.beans;

import io.harness.cdng.common.beans.Tag;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Id;

import java.util.List;

@Data
@Builder
public class K8sDirectInfraDefinition implements InfraDefinition {
  @Id private String uuid;
  private String name;
  private String identifier;
  private String accountId;
  private String description;
  private List<Tag> tags;
  private Spec spec;

  @Override
  public InfraMapping getInfraMapping() {
    return K8sDirectInfraMapping.builder()
        .accountId(accountId)
        .k8sConnector(spec.k8sConnector)
        .namespace(spec.namespace)
        .build();
  }

  @Data
  @Builder
  public static class Spec {
    private String k8sConnector;
    private String namespace;
  }
}
