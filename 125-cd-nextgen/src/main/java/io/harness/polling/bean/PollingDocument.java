package io.harness.polling.bean;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "PollingDocumentKeys")
@Entity(value = "pollingDocuments", noClassnameStored = true)
@Document("pollingDocuments")
@HarnessEntity(exportable = true)
@StoreIn(DbAliases.NG_MANAGER)
@OwnedBy(HarnessTeam.CDC)
public class PollingDocument implements PersistentEntity, AccountAccess, UuidAware {
  @Id @org.mongodb.morphia.annotations.Id private String uuid;

  @NotNull private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  @NotNull private List<String> signature;

  @JsonProperty("type") private PollingType pollingType;

  private PollingInfo pollingInfo;

  private PolledResponse polledResponse;

  private String perpetualTaskId;

  private int failedAttempts;
}
