package software.wings.beans.infrastructure;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;

/**
 * Created by anubhaw on 4/1/16.
 */
@Entity(value = "infrastructure")
@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({ @Type(StaticInfrastructure.class) })
@Indexes(@Index(fields = { @Field("name") }, options = @IndexOptions(unique = true)))
public class Infrastructure extends Base {
  private String name;
  private InfrastructureType type;
  @Transient private HostUsage hostUsage;

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

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets host usage.
   *
   * @return the host usage
   */
  public HostUsage getHostUsage() {
    return hostUsage;
  }

  /**
   * Sets host usage.
   *
   * @param hostUsage the host usage
   */
  public void setHostUsage(HostUsage hostUsage) {
    this.hostUsage = hostUsage;
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
