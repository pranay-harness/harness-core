package software.wings.beans.infrastructure;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

/**
 * Created by anubhaw on 4/1/16.
 */
@Entity(value = "infrastructure")
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({ @Type(StaticInfrastructure.class) })
public class Infrastructure extends Base {
  private String name;
  private InfrastructureType type;

  /**
   * Instantiates a new Infrastructure.
   *
   * @param type the type
   */
  public Infrastructure(InfrastructureType type) {
    this.type = type;
    setAppId(GLOBAL_APP_ID);
  }

  /**
   * Gets infra type.
   *
   * @return the infra type
   */
  public InfrastructureType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * The Enum InfrastructureType.
   */
  public enum InfrastructureType {
    /**
     * Static infra type.
     */
    STATIC, /**
             * Aws infra type.
             */
    AWS, /**
          * Azure infra type.
          */
    AZURE, /**
            * Container infra type.
            */
    CONTAINER
  }
}
