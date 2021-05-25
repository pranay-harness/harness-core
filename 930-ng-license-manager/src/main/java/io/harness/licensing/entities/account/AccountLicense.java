package io.harness.licensing.entities.account;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.Trimmed;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.licensing.ModuleType;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.ng.core.NGAccountAccess;
import io.harness.persistence.PersistentEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
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
@Builder
@FieldNameConstants(innerTypeName = "AccountLicenseKeys")
@Entity(value = "accountLicenses", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@StoreIn(DbAliases.NG_MANAGER)
@Document("accountLicenses")
@Persistent
public class AccountLicense implements PersistentEntity, NGAccountAccess, PersistentRegularIterable {
  @Id @org.mongodb.morphia.annotations.Id private String uuid;
  @Trimmed @NotEmpty private String accountIdentifier;
  Map<ModuleType, ModuleLicense> moduleLicenses;
  boolean allInactive;
  @CreatedBy protected EmbeddedUser createdBy;
  @LastModifiedBy protected EmbeddedUser lastUpdatedBy;
  @CreatedDate protected Long createdAt;
  @LastModifiedDate protected Long lastUpdatedAt;
  @NonFinal @Builder.Default private Long licenseCheckIteration = 0L;

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountIdentifier_accountLicense_unique_index")
                 .fields(Arrays.asList(AccountLicenseKeys.accountIdentifier))
                 .unique(true)
                 .build())
        .build();
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.licenseCheckIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return licenseCheckIteration;
  }
}
