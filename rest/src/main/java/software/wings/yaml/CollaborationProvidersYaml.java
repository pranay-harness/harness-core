package software.wings.yaml;

import java.util.ArrayList;
import java.util.List;

public class CollaborationProvidersYaml extends BaseYaml {
  private List<String> SMTP = new ArrayList<>();
  private List<String> Slack = new ArrayList<>();

  public List<String> getSMTP() {
    return SMTP;
  }

  public void setSMTP(List<String> SMTP) {
    this.SMTP = SMTP;
  }

  public List<String> getSlack() {
    return Slack;
  }

  public void setSlack(List<String> slack) {
    Slack = slack;
  }
}