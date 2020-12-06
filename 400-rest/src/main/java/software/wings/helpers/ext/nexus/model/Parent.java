package software.wings.helpers.ext.nexus.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * Created by sgurubelli on 6/15/17.
 */
@XmlType(name = "parent")
@XmlAccessorType(XmlAccessType.FIELD)
@lombok.Data
public class Parent implements Serializable {
  private String groupId;
  private String artifactId;
  private String version;
}
