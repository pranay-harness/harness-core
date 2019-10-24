package io.harness.queue;

import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;

@Entity(value = "!!!testUnversionedQueue", noClassnameStored = true)
public class TestUnversionedQueuableObject extends Queuable {
  @Getter @Setter private int data;

  public TestUnversionedQueuableObject(int data) {
    this.data = data;
  }
}
