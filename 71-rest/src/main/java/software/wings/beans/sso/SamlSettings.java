package software.wings.beans.sso;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotation.NaturalKey;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class SamlSettings extends SSOSettings {
  @JsonIgnore @NotNull @NaturalKey private String metaDataFile;
  @NotNull @NaturalKey private String accountId;
  @NotNull @NaturalKey private String origin;
  private String groupMembershipAttr;

  @Builder
  public SamlSettings(SSOType ssoType, String displayName, String url, String metaDataFile, String accountId,
      String origin, String groupMembershipAttr) {
    super(SSOType.SAML, displayName, url);
    this.metaDataFile = metaDataFile;
    this.accountId = accountId;
    this.origin = origin;
    this.groupMembershipAttr = groupMembershipAttr;
  }

  @Override
  public SSOSettings getPublicSSOSettings() {
    return this;
  }

  @JsonProperty
  public boolean isAuthorizationEnabled() {
    return isNotEmpty(groupMembershipAttr);
  }
}
