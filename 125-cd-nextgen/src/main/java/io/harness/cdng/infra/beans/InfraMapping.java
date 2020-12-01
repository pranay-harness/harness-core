package io.harness.cdng.infra.beans;

import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.mongodb.morphia.annotations.Entity;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = K8SDirectInfrastructure.class, name = "kubernetes-direct") })
@Entity(value = "infrastructureMapping")
public interface InfraMapping extends PersistentEntity, UuidAware, Outcome {
  void setUuid(String uuid);
  void setAccountId(String accountId);
  void setServiceIdentifier(String serviceIdentifier);
}
