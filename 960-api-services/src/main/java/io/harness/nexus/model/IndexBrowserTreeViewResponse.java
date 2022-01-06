/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.nexus.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.EqualsAndHashCode;
import org.sonatype.nexus.rest.model.NexusResponse;

/**
 * Created by srinivas on 4/4/17.
 */
//@XStreamAlias("content")
@XmlRootElement(name = "content")
@XmlAccessorType(XmlAccessType.FIELD)
@lombok.Data
@EqualsAndHashCode(callSuper = false)
public class IndexBrowserTreeViewResponse extends NexusResponse implements Serializable {
  @XmlElement(name = "data") private Data data;
}
