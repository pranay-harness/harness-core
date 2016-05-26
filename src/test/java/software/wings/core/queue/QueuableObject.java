package software.wings.core.queue;

import com.google.common.base.MoreObjects;

import org.junit.Ignore;
import org.mongodb.morphia.annotations.Entity;

import java.util.Objects;

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
@Ignore
@Entity(value = "testQueue", noClassnameStored = true)
public class QueuableObject extends Queuable {
  private int data;

  public QueuableObject() {
    super();
  }

  public QueuableObject(int data) {
    super();
    this.data = data;
  }

  public QueuableObject(QueuableObject other) {
    super(other);
  }

  public int getData() {
    return data;
  }

  public void setData(int data) {
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
    QueuableObject that = (QueuableObject) obj;
    return data == that.data;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("data", data).toString();
  }
}
