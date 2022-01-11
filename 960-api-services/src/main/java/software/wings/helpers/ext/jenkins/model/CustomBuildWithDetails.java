/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.jenkins.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.offbytwo.jenkins.model.BuildWithDetails;
import java.io.IOException;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@Data
@NoArgsConstructor
public class CustomBuildWithDetails extends BuildWithDetails {
  public CustomBuildWithDetails(BuildWithDetails details) {
    super(details);
  }

  @Override
  public CustomBuildWithDetails details() throws IOException {
    return this.client.get(url, CustomBuildWithDetails.class);
  }

  private String url;
}
