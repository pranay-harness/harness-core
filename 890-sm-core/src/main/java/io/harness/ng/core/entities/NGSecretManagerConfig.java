package io.harness.ng.core.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.DbAliases;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.security.encryption.SecretManagerType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@Data
@Builder
@FieldNameConstants(innerTypeName = "NGSecretManagerConfigKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ngSecretManagerRecords", noClassnameStored = true)
@Document("ngSecretManagerRecords")
@TypeAlias("ngSecretManagerConfig")
@StoreIn(DbAliases.NG_MANAGER)
public class NGSecretManagerConfig implements EncryptionConfig {
    @Id
    @org.mongodb.morphia.annotations.Id String id;

    @NotNull String accountIdentifier;
    String orgIdentifier;
    String projectIdentifier;
    String identifier;
    String name;

    EncryptionType encryptionType;

    boolean isDefault;

    @Override
    @JsonIgnore
    public String getUuid() {
        return this.id;
    }

    @Override
    @JsonIgnore
    public String getAccountId() {
        return this.accountIdentifier;
    }

    @Override
    public int getNumOfEncryptedValue() {
        // not applicable for NG
        return 0;
    }

    @Override
    public String getEncryptionServiceUrl() {
        return null;
    }

    @Override
    public String getValidationCriteria() {
        return null;
    }

    @Override
    public SecretManagerType getType() {
        return null;
    }

    @Override
    public boolean isGlobalKms() {
        return false;
    }
}
