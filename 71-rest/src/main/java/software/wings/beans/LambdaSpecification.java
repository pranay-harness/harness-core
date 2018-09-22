package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.trim;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.yaml.BaseYaml;

import java.util.List;
import javax.validation.Valid;

@Entity("lambdaSpecifications")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class LambdaSpecification extends DeploymentSpecification {
  @NotEmpty @Indexed(options = @IndexOptions(unique = true)) private String serviceId;
  private DefaultSpecification defaults;
  @Valid private List<FunctionSpecification> functions;

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends DeploymentSpecification.Yaml {
    private DefaultSpecification.Yaml defaults;
    private List<FunctionSpecification.Yaml> functions;

    @Builder
    public Yaml(String type, String harnessApiVersion, DefaultSpecification.Yaml defaults,
        List<FunctionSpecification.Yaml> functions) {
      super(type, harnessApiVersion);
      this.defaults = defaults;
      this.functions = functions;
    }
  }

  @Data
  @Builder
  public static class DefaultSpecification {
    @NotBlank private String runtime;
    private Integer memorySize = 128;
    private Integer timeout = 3;
    public String getRuntime() {
      return trim(runtime);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static final class Yaml extends BaseYaml {
      private String runtime;
      private Integer memorySize = 128;
      private Integer timeout = 3;

      @Builder
      public Yaml(String runtime, Integer memorySize, Integer timeout) {
        this.runtime = runtime;
        this.memorySize = memorySize;
        this.timeout = timeout;
      }
    }
  }

  @Data
  @Builder
  public static class FunctionSpecification {
    @NotBlank private String runtime;
    private Integer memorySize = 128;
    private Integer timeout = 3;
    @NotBlank private String functionName;
    @NotBlank private String handler;

    public String getRuntime() {
      return trim(runtime);
    }
    public String getFunctionName() {
      return trim(functionName);
    }
    public String getHandler() {
      return trim(handler);
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static final class Yaml extends BaseYaml {
      private String runtime;
      private Integer memorySize = 128;
      private Integer timeout = 3;
      private String functionName;
      private String handler;

      @Builder
      public Yaml(String runtime, Integer memorySize, Integer timeout, String functionName, String handler) {
        this.runtime = runtime;
        this.memorySize = memorySize;
        this.timeout = timeout;
        this.functionName = functionName;
        this.handler = handler;
      }
    }
  }
}
