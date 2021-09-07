package io.harness.cdng.infra.beans;

import io.harness.annotation.RecasterAlias;
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
@RecasterAlias("io.harness.cdng.infra.beans.K8sGcpInfraMapping")
public class K8sGcpInfraMapping implements InfraMapping {
  @Id private String uuid;
  private String accountId;
  private String gcpConnector;
  private String namespace;
  private String cluster;
}
