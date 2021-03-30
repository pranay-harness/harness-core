package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.MalformedURLException;
import java.net.URL;
import javax.validation.constraints.NotNull;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueKeyNG {
  @NotNull String url;
  @NotNull String key;

  public JiraIssueKeyNG(@NotEmpty String baseUrl, @NotEmpty String key) {
    this.url = prepareUrl(baseUrl, key);
    this.key = key;
  }

  private static String prepareUrl(String baseUrl, String key) {
    try {
      URL issueUrl = new URL(baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "browse/" + key);
      return issueUrl.toString();
    } catch (MalformedURLException e) {
      throw new InvalidRequestException(String.format("Invalid jira base url: %s", baseUrl));
    }
  }
}
