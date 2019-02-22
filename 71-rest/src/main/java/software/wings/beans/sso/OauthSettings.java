package software.wings.beans.sso;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotBlank;

@Data
@EqualsAndHashCode(callSuper = false)
public class OauthSettings extends SSOSettings {
  @NotBlank private String accountId;
  private String filter;

  @JsonCreator
  @Builder
  public OauthSettings(@JsonProperty("displayName") String displayName, @JsonProperty("url") String url,
      @JsonProperty("accountid") String accountId, @JsonProperty("filter") String filter) {
    super(SSOType.OAUTH, displayName, url);
    this.accountId = accountId;
    this.filter = filter;
  }

  @Override
  public SSOSettings getPublicSSOSettings() {
    return this;
  }
}
