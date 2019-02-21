package software.wings.beans.security.access;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.harness.annotation.NaturalKey;
import io.harness.beans.EmbeddedUser;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.beans.Base;

/**
 * @author rktummala on 04/06/2018
 */

@JsonInclude(NON_EMPTY)
@Entity(value = "whitelist", noClassnameStored = true)
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Whitelist extends Base {
  @Indexed @NotEmpty @NaturalKey private String accountId;
  private String description;
  @NotEmpty @NaturalKey private WhitelistStatus status = WhitelistStatus.ACTIVE;
  @NotEmpty @NaturalKey private String filter;

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
