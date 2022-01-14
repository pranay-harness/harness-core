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
