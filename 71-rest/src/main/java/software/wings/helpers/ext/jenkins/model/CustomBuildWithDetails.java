package software.wings.helpers.ext.jenkins.model;

import com.offbytwo.jenkins.model.BuildWithDetails;
import java.io.IOException;
import lombok.Data;
import lombok.NoArgsConstructor;

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
