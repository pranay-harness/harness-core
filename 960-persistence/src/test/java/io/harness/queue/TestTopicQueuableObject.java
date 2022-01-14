package io.harness.queue;

import lombok.Value;
import org.mongodb.morphia.annotations.Entity;

@Value
@Entity(value = "!!!testTopicQueue", noClassnameStored = true)
public class TestTopicQueuableObject extends Queuable {
  private int data;

  public TestTopicQueuableObject(int data) {
    this.data = data;
  }
}
