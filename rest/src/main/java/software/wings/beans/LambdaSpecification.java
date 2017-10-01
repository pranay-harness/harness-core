package software.wings.beans;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.utils.Misc;

@Entity("lambdaSpecifications")
@Data
@Builder
@Indexes(@Index(fields = { @Field("serviceId")
                           , @Field("deploymentType") }, options = @IndexOptions(unique = true)))
public class LambdaSpecification extends Base {
  @NotEmpty private String serviceId;
  @NotEmpty private String bucket;
  @NotEmpty private String key;
  @NotEmpty private String runtime;
  private Integer memorySize = 128;
  private Integer timeout = 3;
  @NotEmpty private String functionName;
  @NotEmpty private String handler;
  @NotEmpty private String role;

  public String getBucket() {
    return Misc.trim(bucket);
  }

  public String getKey() {
    return Misc.trim(key);
  }

  public String getRuntime() {
    return Misc.trim(runtime);
  }

  public String getFunctionName() {
    return Misc.trim(functionName);
  }

  public String getHandler() {
    return Misc.trim(handler);
  }

  public String getRole() {
    return Misc.trim(role);
  }
}
