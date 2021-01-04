package io.harness.beans.sweepingoutputs;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "podDetails")
@HarnessEntity(exportable = true)
@TypeAlias("k8PodDetails")
@JsonTypeName("k8PodDetails")
public class K8PodDetails implements PersistentEntity, UuidAware, ContextElement, AccountAccess {
  private String namespace;
  private String stageID;
  private String clusterName;
  private long lastUpdatedAt;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @FdIndex private String accountId;

  @Override
  public String getType() {
    return "k8PodDetails";
  }
}
