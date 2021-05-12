package io.harness.licensing.entities.modules;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.NGAccountAccess;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.GTM)
@Data
@FieldNameConstants(innerTypeName = "ModuleLicenseKeys")
@Entity(value = "moduleLicenses", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@StoreIn(DbAliases.NG_MANAGER)
@Document("moduleLicenses")
@Persistent
public abstract class ModuleLicense implements PersistentEntity, NGAccountAccess {
  @Id @org.mongodb.morphia.annotations.Id protected String id;
  @Trimmed @NotEmpty protected String accountIdentifier;
  @NotEmpty protected ModuleType moduleType;
  @NotEmpty protected Edition edition;
  @NotEmpty protected LicenseType licenseType;
  @NotEmpty protected long startTime;
  @NotEmpty protected long expiryTime;
  @NotEmpty protected LicenseStatus status;
  @CreatedBy protected EmbeddedUser createdBy;
  @LastModifiedBy protected EmbeddedUser lastUpdatedBy;
  @CreatedDate protected Long createdAt;
  @LastModifiedDate protected Long lastUpdatedAt;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountIdentifier_moduleLicense_query_index")
                 .fields(Arrays.asList(ModuleLicenseKeys.accountIdentifier))
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("unique_accountIdentifier_modulytype_index")
                 .unique(true)
                 .fields(Arrays.asList(ModuleLicenseKeys.accountIdentifier, ModuleLicenseKeys.moduleType))
                 .build())
        .build();
  }
}
