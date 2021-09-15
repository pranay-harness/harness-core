/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.scim;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ScimListResponse<T> {
  @JsonProperty(value = "schemas")
  private Set<String> schemas = new HashSet<>(Arrays.asList("urn:ietf:params:scim:api:messages:2.0:ListResponse"));
  @JsonProperty(value = "totalResults", required = true) private Integer totalResults;
  @JsonProperty(value = "Resources", required = true) private List<T> resources = new LinkedList();
  @JsonProperty("startIndex") private Integer startIndex;
  @JsonProperty("itemsPerPage") private Integer itemsPerPage;

  public ScimListResponse() {}

  public void startIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public void itemsPerPage(int itemsPerPage) {
    this.itemsPerPage = itemsPerPage;
  }

  public void totalResults(int totalResults) {
    this.totalResults = totalResults;
  }

  public boolean resource(T scimResource) {
    this.resources.add(scimResource);
    return true;
  }
}
