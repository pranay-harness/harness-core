package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(PL)
@Data
@EqualsAndHashCode(callSuper = true)
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

  @Override
  public SSOType getType() {
    return SSOType.SAML;
  }

  @JsonProperty
  public boolean isAuthorizationEnabled() {
    return isNotEmpty(groupMembershipAttr);
  }
}
