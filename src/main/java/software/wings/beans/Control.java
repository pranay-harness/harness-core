package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

@Entity(value = "controls", noClassnameStored = true)
public class Control extends Execution {
  @Override
  public String getCommand() {
    return null;
  }
}
