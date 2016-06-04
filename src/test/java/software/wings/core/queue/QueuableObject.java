package software.wings.core.queue;

import com.google.common.base.MoreObjects;

import org.junit.Ignore;
import org.mongodb.morphia.annotations.Entity;

import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 4/13/16.
 */
@Ignore
@Entity(value = "testQueue", noClassnameStored = true)
public class QueuableObject extends Queuable {
  private int data;

  /**
   * Instantiates a new queuable object.
   */
  public QueuableObject() {
    super();
  }

  /**
   * Instantiates a new queuable object.
   *
   * @param data the data
   */
  public QueuableObject(int data) {
    super();
    this.data = data;
  }

  /**
   * Instantiates a new queuable object.
   *
   * @param other the other
   */
  public QueuableObject(QueuableObject other) {
    super(other);
  }

  public int getData() {
    return data;
  }

  public void setData(int data) {
    this.data = data;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(data);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
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

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("data", data).toString();
  }
}
