package software.wings.security.authentication;

/**
 * See `SimpleUrlBuilderTest` for usage example.
 * For anything more sophisticated prefer {@link org.apache.http.client.utils.URIBuilder}
 */
public class SimpleUrlBuilder {
  private String url;
  private boolean hasQueryParams;

  public SimpleUrlBuilder(String baseUrl) {
    this.url = baseUrl;
  }

  public SimpleUrlBuilder addQueryParam(String key, String value) {
    StringBuilder builder = new StringBuilder();
    if (this.hasQueryParams) {
      builder.append('&');
    } else {
      builder.append('?');
    }

    builder.append(key).append('=').append(value);

    this.hasQueryParams = true;
    this.url += builder.toString();

    return this;
  }

  public String build() {
    return this.url;
  }
}
