package software.wings.waitnotify;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 6/24/16.
 */
public class StringNotifyResponseData implements NotifyResponseData {
  private String data;

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  @Override
  public int hashCode() {
    return Objects.hash(data);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final StringNotifyResponseData other = (StringNotifyResponseData) obj;
    return Objects.equals(this.data, other.data);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("data", data).toString();
  }

  public static final class Builder {
    private String data;

    private Builder() {}

    public static Builder aStringNotifyResponseData() {
      return new Builder();
    }

    public Builder withData(String data) {
      this.data = data;
      return this;
    }

    public Builder but() {
      return aStringNotifyResponseData().withData(data);
    }

    public StringNotifyResponseData build() {
      StringNotifyResponseData stringNotifyResponseData = new StringNotifyResponseData();
      stringNotifyResponseData.setData(data);
      return stringNotifyResponseData;
    }
  }
}
