package software.wings.helpers.ext.nexus.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by srinivas on 4/6/17.
 */

@XmlType(name = "project")
@XmlAccessorType(XmlAccessType.FIELD)
@lombok.Data
public class Project implements Serializable {
  private String modelVersion;
  private String groupId;
  private String artifactId;
  private String version;
  private String packaging;
  private String description;

  private Parent parent;
}
