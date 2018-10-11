package software.wings.delegatetasks;

import com.google.common.base.MoreObjects;

import com.esotericsoftware.kryo.serializers.FieldSerializer.Bind;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import io.harness.task.protocol.ResponseData;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 1/12/17.
 */

public class RemoteMethodReturnValueData implements ResponseData {
  private Object returnValue;

  @Bind(JavaSerializer.class) private Throwable exception;

  public RemoteMethodReturnValueData() {}

  /**
   * Getter for property 'returnValue'.
   *
   * @return Value for property 'returnValue'.
   */
  public Object getReturnValue() {
    return returnValue;
  }

  /**
   * Setter for property 'returnValue'.
   *
   * @param returnValue Value to set for property 'returnValue'.
   */
  public void setReturnValue(Object returnValue) {
    this.returnValue = returnValue;
  }

  /**
   * Getter for property 'exception'.
   *
   * @return Value for property 'exception'.
   */
  public Throwable getException() {
    return exception;
  }

  /**
   * Setter for property 'exception'.
   *
   * @param exception Value to set for property 'exception'.
   */
  public void setException(Throwable exception) {
    this.exception = exception;
  }

  @Override
  public int hashCode() {
    return Objects.hash(returnValue, exception);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final RemoteMethodReturnValueData other = (RemoteMethodReturnValueData) obj;
    return Objects.equals(this.returnValue, other.returnValue) && Objects.equals(this.exception, other.exception);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("returnValue", returnValue).add("exception", exception).toString();
  }

  public static final class Builder {
    private Object returnValue;
    private Throwable exception;

    private Builder() {}

    public static Builder aRemoteMethodReturnValueData() {
      return new Builder();
    }

    public Builder withReturnValue(Object returnValue) {
      this.returnValue = returnValue;
      return this;
    }

    public Builder withException(Throwable exception) {
      this.exception = exception;
      return this;
    }

    public Builder but() {
      return aRemoteMethodReturnValueData().withReturnValue(returnValue).withException(exception);
    }

    public RemoteMethodReturnValueData build() {
      RemoteMethodReturnValueData remoteMethodReturnValueData = new RemoteMethodReturnValueData();
      remoteMethodReturnValueData.setReturnValue(returnValue);
      remoteMethodReturnValueData.setException(exception);
      return remoteMethodReturnValueData;
    }
  }
}
