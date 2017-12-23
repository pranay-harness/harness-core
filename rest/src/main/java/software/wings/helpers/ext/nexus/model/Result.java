package software.wings.helpers.ext.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 11/17/17.
 */
@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result {
  private boolean success;
  private List<ResponseData> data = new ArrayList<>();
}
