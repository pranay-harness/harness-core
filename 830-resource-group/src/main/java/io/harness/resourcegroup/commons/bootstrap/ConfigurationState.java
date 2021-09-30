package io.harness.resourcegroup.commons.bootstrap;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.DbAliases.RESOURCEGROUP;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "ConfigurationStateKeys")
@Entity(value = "configurationState", noClassnameStored = true)
@Document("configurationState")
@TypeAlias("configurationState")
@StoreIn(RESOURCEGROUP)
public class ConfigurationState {
  @Id @org.mongodb.morphia.annotations.Id String id;
  @FdIndex @NotEmpty String identifier;
  int configVersion;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long documentVersion;
}
