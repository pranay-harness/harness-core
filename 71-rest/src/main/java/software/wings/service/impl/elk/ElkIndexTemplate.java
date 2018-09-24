package software.wings.service.impl.elk;

import lombok.Data;

import java.util.Map;

/**
 * Created by rsingh on 8/23/17.
 */
@Data
public class ElkIndexTemplate {
  private String name;
  private Map<String, Object> properties;
}
