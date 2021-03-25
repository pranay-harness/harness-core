package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(PL)
@Data
@EqualsAndHashCode(callSuper = false)
public class SamlSettings extends SSOSettings {
  @JsonIgnore @NotNull private String metaDataFile;
  @NotNull private String accountId;
  @NotNull private String origin;
  private String logoutUrl;
  private String groupMembershipAttr;

  @Builder
  public SamlSettings(SSOType ssoType, String displayName, String url, String metaDataFile, String accountId,
      String origin, String groupMembershipAttr, String logoutUrl) {
    super(SSOType.SAML, displayName, url);
    this.metaDataFile = metaDataFile;
    this.accountId = accountId;
    this.origin = origin;
    this.groupMembershipAttr = groupMembershipAttr;
    this.logoutUrl = logoutUrl;
  }

  @Override
  public SSOSettings getPublicSSOSettings() {
    return this;
  }

  @JsonProperty
  public boolean isAuthorizationEnabled() {
    return hasSome(groupMembershipAttr);
  }
}
