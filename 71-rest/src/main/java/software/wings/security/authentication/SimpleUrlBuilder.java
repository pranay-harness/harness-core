package software.wings.security.authentication;

public class SimpleUrlBuilder {
  private StringBuilder builder;
  private boolean hasQueryParams;

  public SimpleUrlBuilder(String baseUrl) {
    this.builder = new StringBuilder(baseUrl);
  }

  public SimpleUrlBuilder addQueryParam(String key, String value) {
    if (this.hasQueryParams) {
      builder.append('&');
    } else {
      builder.append('?');
    }

    builder.append(key).append('=').append(value);
    this.hasQueryParams = true;
    return this;
  }

  public String build() {
    return this.builder.toString();
  }
}
