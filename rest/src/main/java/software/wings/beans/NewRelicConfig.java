package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import ro.fortsoft.pf4j.Extension;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.service.impl.newrelic.NewRelicUrlProvider;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

/**
 * Created by raghu on 8/28/17.
 */
@Extension
@JsonTypeName("NEW_RELIC")
@Data
@Builder
@ToString(exclude = "apiKey")
public class NewRelicConfig extends SettingValue implements Encryptable {
  @EnumData(enumDataProvider = NewRelicUrlProvider.class)
  @Attributes(title = "URL")
  @NotEmpty
  @DefaultValue("https://api.newrelic.com")
  private String newRelicUrl = "https://api.newrelic.com";

  @Attributes(title = "API key", required = true)
  @NotEmpty
  @Encrypted
  @JsonView(JsonViews.Internal.class)
  private char[] apiKey;

  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new App dynamics config.
   */
  public NewRelicConfig() {
    super(StateType.NEW_RELIC.name());
  }

  public NewRelicConfig(String newRelicUrl, char[] apiKey, String accountId) {
    super(StateType.NEW_RELIC.name());
    this.newRelicUrl = newRelicUrl;
    this.apiKey = apiKey;
    this.accountId = accountId;
  }
}
