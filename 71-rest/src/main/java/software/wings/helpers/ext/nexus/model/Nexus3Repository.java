package software.wings.helpers.ext.nexus.model;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;

/**
 * Created by Aaditi Joag on 7/2/19.
 */
@OwnedBy(CDC)
@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3Repository {
  private String name;
  private String format;
  private String type;
  private String url;
}
