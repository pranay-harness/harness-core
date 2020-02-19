package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class AwsVPC implements Serializable {
  private String id;
  private String name;
}
