package io.harness.pms.downloadlogs.beans.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.data.validator.Trimmed;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.Date;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "DownloadLogsEntityKeys")
@Entity(value = "DownloadLogsLinks", noClassnameStored = true)
@Document("DownloadLogsLinks")
@TypeAlias("DownloadLogsLinks")
@HarnessEntity(exportable = true)
public class DownloadLogsEntity implements PersistentEntity, AccountAccess, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Setter @NonFinal @Id @org.mongodb.morphia.annotations.Id String uuid;

  @NotEmpty @FdUniqueIndex String logKey;
  @NotEmpty @FdTtlIndex Date validUntil;

  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @Trimmed @NotEmpty String projectIdentifier;
  @Trimmed @NotEmpty String pipelineIdentifier;

  @Setter @NonFinal @SchemaIgnore @FdIndex @CreatedDate long createdAt;
  @Setter @NonFinal @SchemaIgnore @NotNull @LastModifiedDate long lastUpdatedAt;

  @Setter @NonFinal @Version Long version;

  @Override
  public String getAccountId() {
    return accountId;
  }
}
