package io.harness.queue;

import lombok.Value;
import org.mongodb.morphia.annotations.Entity;

@Value
@Entity(value = "!!!testVersionedQueue", noClassnameStored = true)
public class TestVersionedQueuableObject extends Queuable {
  private int data;

  public TestVersionedQueuableObject(int data) {
    this.data = data;
  }
}
