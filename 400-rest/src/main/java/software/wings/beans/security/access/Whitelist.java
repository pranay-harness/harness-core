package software.wings.beans.security.access;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;

import software.wings.beans.Base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;

/**
 * @author rktummala on 04/06/2018
 */
@OwnedBy(PL)
@JsonInclude(NON_EMPTY)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "WhitelistKeys")
@Entity(value = "whitelist", noClassnameStored = true)
@HarnessEntity(exportable = true)
public final class Whitelist extends Base implements AccountAccess {
  @FdIndex @NotEmpty private String accountId;
  private String description;
  @NotEmpty private WhitelistStatus status = WhitelistStatus.ACTIVE;
  @NotEmpty private String filter;

  @Builder
  public Whitelist(String uuid, String appId, EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy,
      long lastUpdatedAt, String entityYamlPath, String accountId, String description, WhitelistStatus status,
      String filter) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.accountId = accountId;
    this.description = description;
    this.status = status;
    this.filter = filter;
  }
}
