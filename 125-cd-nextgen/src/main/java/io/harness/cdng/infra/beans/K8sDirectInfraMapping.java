package io.harness.cdng.infra.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("k8sDirectInfraMapping")
@JsonTypeName("k8sDirectInfraMapping")
@OwnedBy(CDC)
@RecasterAlias("io.harness.cdng.infra.beans.K8sDirectInfraMapping")
public class K8sDirectInfraMapping implements InfraMapping {
  @Id private String uuid;
  private String accountId;
  private String k8sConnector;
  private String namespace;
}
