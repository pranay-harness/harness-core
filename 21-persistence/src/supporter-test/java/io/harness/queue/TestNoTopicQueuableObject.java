package io.harness.queue;

import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;

@Entity(value = "!!!testNoTopicQueue", noClassnameStored = true)
public class TestNoTopicQueuableObject extends Queuable {
  @Getter @Setter private int data;

  public TestNoTopicQueuableObject(int data) {
    this.data = data;
  }
}
