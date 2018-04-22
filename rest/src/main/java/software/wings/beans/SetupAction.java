package software.wings.beans;

import com.google.common.base.MoreObjects;

import io.harness.eraro.Level;

import java.util.Objects;

/**
 * Created by anubhaw on 6/30/16.
 */
public class SetupAction {
  private String displayText;
  private String code;
  private String url;
  private Level errorType;

  /**
   * Gets display text.
   *
   * @return the display text
   */
  public String getDisplayText() {
    return displayText;
  }

  /**
   * Sets display text.
   *
   * @param displayText the display text
   */
  public void setDisplayText(String displayText) {
    this.displayText = displayText;
  }

  /**
   * Gets code.
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Sets code.
   *
   * @param code the code
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Gets url.
   *
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets url.
   *
   * @param url the url
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * Gets error type.
   *
   * @return the error type
   */
  public Level getErrorType() {
    return errorType;
  }

  /**
   * Sets error type.
   *
   * @param errorType the error type
   */
  public void setErrorType(Level errorType) {
    this.errorType = errorType;
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

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String displayText;
    private String code;
    private String url;

    private Builder() {}

    /**
     * A setup action builder.
     *
     * @return the builder
     */
    public static Builder aSetupAction() {
      return new Builder();
    }

    /**
     * With display text builder.
     *
     * @param displayText the display text
     * @return the builder
     */
    public Builder withDisplayText(String displayText) {
      this.displayText = displayText;
      return this;
    }

    /**
     * With code builder.
     *
     * @param code the code
     * @return the builder
     */
    public Builder withCode(String code) {
      this.code = code;
      return this;
    }

    /**
     * With url builder.
     *
     * @param url the url
     * @return the builder
     */
    public Builder withUrl(String url) {
      this.url = url;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aSetupAction().withDisplayText(displayText).withCode(code).withUrl(url);
    }

    /**
     * Build setup action.
     *
     * @return the setup action
     */
    public SetupAction build() {
      SetupAction setupAction = new SetupAction();
      setupAction.setDisplayText(displayText);
      setupAction.setCode(code);
      setupAction.setUrl(url);
      return setupAction;
    }
  }
}
