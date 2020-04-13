package io.harness.governance.pipeline.service.model;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotation.HarnessEntity;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.UUIDGenerator;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Entity representing a pipeline governance standard.
 */
@Value
@Entity(value = "pipelineGovernanceConfigs")
@HarnessEntity(exportable = true)
@Indexes(@Index(fields = @Field("accountId"), options = @IndexOptions(name = "account_id_idx")))
@FieldNameConstants(innerTypeName = "PipelineGovernanceConfigKeys")
@ParametersAreNonnullByDefault
public class PipelineGovernanceConfig implements PersistentEntity, UuidAccess, AccountAccess {
  @Id private String uuid;
  @Nonnull private String accountId;
  @Nonnull private String name;
  @Nonnull private String description;
  @Nonnull private List<PipelineGovernanceRule> rules;
  @Nonnull private List<Restriction> restrictions;
  private boolean enabled;

  @JsonCreator
  public PipelineGovernanceConfig(@Nullable @JsonProperty("uuid") String uuid,
      @JsonProperty("accountId") String accountId, @JsonProperty("name") String name,
      @JsonProperty("description") String description, @JsonProperty("rules") List<PipelineGovernanceRule> rules,
      @JsonProperty("restrictions") List<Restriction> restrictions, @JsonProperty("enabled") boolean enabled) {
    if (null == uuid) {
      this.uuid = UUIDGenerator.generateUuid();
    } else {
      this.uuid = uuid;
    }
    this.description = trimToEmpty(description);
    this.accountId = Objects.requireNonNull(accountId, "accountId must be present");
    this.name = name;
    this.rules = rules;
    this.restrictions = restrictions;
    this.enabled = enabled;
  }

  public List<PipelineGovernanceRule> getRules() {
    return CollectionUtils.emptyIfNull(rules);
  }

  @Nonnull
  public List<Restriction> getRestrictions() {
    return CollectionUtils.emptyIfNull(restrictions);
  }

  @Override
  @Nonnull
  public String getUuid() {
    return uuid;
  }
}
