/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Created by rsingh on 8/28/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class NewRelicApplication implements Comparable<NewRelicApplication> {
  private String name;
  private long id;

  @Override
  public int compareTo(NewRelicApplication o) {
    return name.compareTo(o.name);
  }

  @Data
  @Builder
  public static class NewRelicApplications {
    private List<NewRelicApplication> applications;
  }
}
