package software.wings.security;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntityInfo {
  private String id;
  private String name;
}
