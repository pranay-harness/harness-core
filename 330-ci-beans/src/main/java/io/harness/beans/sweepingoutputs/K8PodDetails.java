package io.harness.beans.sweepingoutputs;

import io.harness.annotation.HarnessEntity;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "podDetails")
@HarnessEntity(exportable = true)
public class K8PodDetails implements PersistentEntity, UuidAware, ContextElement, AccountAccess {
  private String namespace;
  private BuildNumberDetails buildNumberDetails;
  private String stageID;
  private String clusterName;
  private long lastUpdatedAt;
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @FdIndex private String accountId;
}
