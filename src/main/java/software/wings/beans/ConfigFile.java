package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;

/**
 * Created by anubhaw on 4/12/16.
 */
@Entity(value = "configFiles", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("serviceId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class ConfigFile extends BaseFile {
  private String serviceId;
  private String relativePath;

  public ConfigFile() {}

  public ConfigFile(String serviceId, String fileName, String relativePath, String md5) {
    super(fileName, md5);
    this.serviceId = serviceId;
    this.relativePath = relativePath;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public void setRelativePath(String relativePath) {
    this.relativePath = relativePath;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(serviceId, relativePath);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final ConfigFile other = (ConfigFile) obj;
    return Objects.equals(this.serviceId, other.serviceId) && Objects.equals(this.relativePath, other.relativePath);
  }
}
