package io.harness.cdng.infra.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.data.Outcome;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import org.mongodb.morphia.annotations.Entity;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @JsonSubTypes.Type(value = K8sDirectInfraDefinition.class, name = "kubernetes-direct") })
@Entity(value = "infrastructureMapping")
public interface InfraMapping extends PersistentEntity, UuidAware, Outcome {
  void setUuid(String uuid);
  void setAccountId(String accountId);
  void setServiceIdentifier(String serviceIdentifier);
}
