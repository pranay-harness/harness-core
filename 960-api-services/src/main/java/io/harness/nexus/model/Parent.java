package io.harness.nexus.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

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
@OwnedBy(HarnessTeam.CDC)
public class Parent implements Serializable {
  private String groupId;
  private String artifactId;
  private String version;
}
