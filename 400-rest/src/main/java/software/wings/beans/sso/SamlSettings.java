package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("SAML")
public class SamlSettings extends SSOSettings {
  @JsonIgnore @NotNull private String metaDataFile;
  @NotNull private String accountId;
  @NotNull private String origin;
  private String logoutUrl;
  private String groupMembershipAttr;
  private String entityIdentifier;

  @JsonCreator
  @Builder
  public SamlSettings(@JsonProperty("type") SSOType ssoType, @JsonProperty("displayName") String displayName,
      @JsonProperty("url") String url, @JsonProperty("metaDataFile") String metaDataFile,
      @JsonProperty("accountId") String accountId, @JsonProperty("origin") String origin,
      @JsonProperty("groupMembershipAttr") String groupMembershipAttr, @JsonProperty("logoutUrl") String logoutUrl,
      @JsonProperty() String entityIdentifier) {
    super(SSOType.SAML, displayName, url);
    this.metaDataFile = metaDataFile;
    this.accountId = accountId;
    this.origin = origin;
    this.groupMembershipAttr = groupMembershipAttr;
    this.logoutUrl = logoutUrl;
    this.entityIdentifier = entityIdentifier;
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