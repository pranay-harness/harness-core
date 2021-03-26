package io.harness.cdng.infra.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("k8sGcpInfraMapping")
@JsonTypeName("k8sGcpInfraMapping")
@OwnedBy(HarnessTeam.CDP)
public class K8sGcpInfraMapping implements InfraMapping {
  @Id private String uuid;
  private String accountId;
  private String k8sConnector;
  private String namespace;
  private String cluster;

  @Override
  public String getType() {
    return "k8sGcpInfraMapping";
  }
}
