package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.authentication.OauthProviderType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(PL)
@Data
@EqualsAndHashCode(callSuper = false)
public class OauthSettings extends SSOSettings {
  @NotBlank private String accountId;
  private String filter;
  private Set<OauthProviderType> allowedProviders;

  @JsonCreator
  @Builder
  public OauthSettings(@JsonProperty("accountid") String accountId, @JsonProperty("displayName") String displayName,
      @JsonProperty("filter") String filter,
      @JsonProperty("allowedProviders") Set<OauthProviderType> allowedProviders) {
    super(SSOType.OAUTH, allowedProviders.stream().map(OauthProviderType::name).collect(Collectors.joining(",")), "");
    this.accountId = accountId;
    this.filter = filter;
    if (isNotEmpty(displayName)) {
      this.displayName = displayName;
    }
    this.allowedProviders = allowedProviders;
  }

  @Override
  public SSOSettings getPublicSSOSettings() {
    return this;
  }
}
