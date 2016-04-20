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
public class TestQueuable extends Queuable {
  private int data;

  public TestQueuable() {
    super();
  }

  public TestQueuable(int data) {
    super();
    this.data = data;
  }

  public TestQueuable(TestQueuable other) {
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
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    TestQueuable that = (TestQueuable) o;
    return data == that.data;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("data", data).toString();
  }
}
