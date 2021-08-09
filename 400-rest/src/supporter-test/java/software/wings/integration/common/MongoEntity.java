package software.wings.integration.common;

import software.wings.beans.Base;

import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;

@Entity(value = "!!!testMongo", noClassnameStored = true)
public class MongoEntity extends Base {
  @Getter @Setter private String data;
}
