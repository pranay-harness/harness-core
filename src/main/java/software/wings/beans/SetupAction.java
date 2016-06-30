package software.wings.beans;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by anubhaw on 6/30/16.
 */
public class SetupAction {
  private String displayText;
  private String code;
  private String url;

  public String getDisplayText() {
    return displayText;
  }

  public void setDisplayText(String displayText) {
    this.displayText = displayText;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayText, code, url);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final SetupAction other = (SetupAction) obj;
    return Objects.equals(this.displayText, other.displayText) && Objects.equals(this.code, other.code)
        && Objects.equals(this.url, other.url);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("displayText", displayText)
        .add("code", code)
        .add("url", url)
        .toString();
  }

  public static final class Builder {
    private String displayText;
    private String code;
    private String url;

    private Builder() {}

    public static Builder aSetupAction() {
      return new Builder();
    }

    public Builder withDisplayText(String displayText) {
      this.displayText = displayText;
      return this;
    }

    public Builder withCode(String code) {
      this.code = code;
      return this;
    }

    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder but() {
      return aSetupAction().withDisplayText(displayText).withCode(code).withUrl(url);
    }

    public SetupAction build() {
      SetupAction setupAction = new SetupAction();
      setupAction.setDisplayText(displayText);
      setupAction.setCode(code);
      setupAction.setUrl(url);
      return setupAction;
    }
  }
}
