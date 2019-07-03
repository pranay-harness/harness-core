package software.wings.helpers.ext.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Aaditi Joag on 7/2/19.
 */
@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3Repository {
  private String name;
  private String format;
  private String type;
  private String url;
}
