package software.wings.api;

/**
 * Created by rishi on 10/6/16.
 */
public class WorkflowElement {
  private String uuid;
  private String name;
  private String url;

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public static final class WorkflowElementBuilder {
    private String uuid;
    private String name;
    private String url;

    private WorkflowElementBuilder() {}

    public static WorkflowElementBuilder aWorkflowElement() {
      return new WorkflowElementBuilder();
    }

    public WorkflowElementBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public WorkflowElementBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public WorkflowElementBuilder withUrl(String url) {
      this.url = url;
      return this;
    }

    public WorkflowElement build() {
      WorkflowElement workflowElement = new WorkflowElement();
      workflowElement.setUuid(uuid);
      workflowElement.setName(name);
      workflowElement.setUrl(url);
      return workflowElement;
    }
  }
}
