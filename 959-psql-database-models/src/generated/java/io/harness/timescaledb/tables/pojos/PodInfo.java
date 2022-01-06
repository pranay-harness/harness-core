/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.pojos;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class PodInfo implements Serializable {
  private static final long serialVersionUID = 1L;

  private String accountid;
  private String clusterid;
  private String instanceid;
  private OffsetDateTime starttime;
  private OffsetDateTime stoptime;
  private String parentnodeid;
  private String namespace;
  private String name;
  private Double cpurequest;
  private Double memoryrequest;
  private String workloadid;
  private OffsetDateTime createdat;
  private OffsetDateTime updatedat;

  public PodInfo() {}

  public PodInfo(PodInfo value) {
    this.accountid = value.accountid;
    this.clusterid = value.clusterid;
    this.instanceid = value.instanceid;
    this.starttime = value.starttime;
    this.stoptime = value.stoptime;
    this.parentnodeid = value.parentnodeid;
    this.namespace = value.namespace;
    this.name = value.name;
    this.cpurequest = value.cpurequest;
    this.memoryrequest = value.memoryrequest;
    this.workloadid = value.workloadid;
    this.createdat = value.createdat;
    this.updatedat = value.updatedat;
  }

  public PodInfo(String accountid, String clusterid, String instanceid, OffsetDateTime starttime,
      OffsetDateTime stoptime, String parentnodeid, String namespace, String name, Double cpurequest,
      Double memoryrequest, String workloadid, OffsetDateTime createdat, OffsetDateTime updatedat) {
    this.accountid = accountid;
    this.clusterid = clusterid;
    this.instanceid = instanceid;
    this.starttime = starttime;
    this.stoptime = stoptime;
    this.parentnodeid = parentnodeid;
    this.namespace = namespace;
    this.name = name;
    this.cpurequest = cpurequest;
    this.memoryrequest = memoryrequest;
    this.workloadid = workloadid;
    this.createdat = createdat;
    this.updatedat = updatedat;
  }

  /**
   * Getter for <code>public.pod_info.accountid</code>.
   */
  public String getAccountid() {
    return this.accountid;
  }

  /**
   * Setter for <code>public.pod_info.accountid</code>.
   */
  public PodInfo setAccountid(String accountid) {
    this.accountid = accountid;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.clusterid</code>.
   */
  public String getClusterid() {
    return this.clusterid;
  }

  /**
   * Setter for <code>public.pod_info.clusterid</code>.
   */
  public PodInfo setClusterid(String clusterid) {
    this.clusterid = clusterid;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.instanceid</code>.
   */
  public String getInstanceid() {
    return this.instanceid;
  }

  /**
   * Setter for <code>public.pod_info.instanceid</code>.
   */
  public PodInfo setInstanceid(String instanceid) {
    this.instanceid = instanceid;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.starttime</code>.
   */
  public OffsetDateTime getStarttime() {
    return this.starttime;
  }

  /**
   * Setter for <code>public.pod_info.starttime</code>.
   */
  public PodInfo setStarttime(OffsetDateTime starttime) {
    this.starttime = starttime;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.stoptime</code>.
   */
  public OffsetDateTime getStoptime() {
    return this.stoptime;
  }

  /**
   * Setter for <code>public.pod_info.stoptime</code>.
   */
  public PodInfo setStoptime(OffsetDateTime stoptime) {
    this.stoptime = stoptime;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.parentnodeid</code>.
   */
  public String getParentnodeid() {
    return this.parentnodeid;
  }

  /**
   * Setter for <code>public.pod_info.parentnodeid</code>.
   */
  public PodInfo setParentnodeid(String parentnodeid) {
    this.parentnodeid = parentnodeid;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.namespace</code>.
   */
  public String getNamespace() {
    return this.namespace;
  }

  /**
   * Setter for <code>public.pod_info.namespace</code>.
   */
  public PodInfo setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.name</code>.
   */
  public String getName() {
    return this.name;
  }

  /**
   * Setter for <code>public.pod_info.name</code>.
   */
  public PodInfo setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.cpurequest</code>.
   */
  public Double getCpurequest() {
    return this.cpurequest;
  }

  /**
   * Setter for <code>public.pod_info.cpurequest</code>.
   */
  public PodInfo setCpurequest(Double cpurequest) {
    this.cpurequest = cpurequest;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.memoryrequest</code>.
   */
  public Double getMemoryrequest() {
    return this.memoryrequest;
  }

  /**
   * Setter for <code>public.pod_info.memoryrequest</code>.
   */
  public PodInfo setMemoryrequest(Double memoryrequest) {
    this.memoryrequest = memoryrequest;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.workloadid</code>.
   */
  public String getWorkloadid() {
    return this.workloadid;
  }

  /**
   * Setter for <code>public.pod_info.workloadid</code>.
   */
  public PodInfo setWorkloadid(String workloadid) {
    this.workloadid = workloadid;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.createdat</code>.
   */
  public OffsetDateTime getCreatedat() {
    return this.createdat;
  }

  /**
   * Setter for <code>public.pod_info.createdat</code>.
   */
  public PodInfo setCreatedat(OffsetDateTime createdat) {
    this.createdat = createdat;
    return this;
  }

  /**
   * Getter for <code>public.pod_info.updatedat</code>.
   */
  public OffsetDateTime getUpdatedat() {
    return this.updatedat;
  }

  /**
   * Setter for <code>public.pod_info.updatedat</code>.
   */
  public PodInfo setUpdatedat(OffsetDateTime updatedat) {
    this.updatedat = updatedat;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final PodInfo other = (PodInfo) obj;
    if (accountid == null) {
      if (other.accountid != null)
        return false;
    } else if (!accountid.equals(other.accountid))
      return false;
    if (clusterid == null) {
      if (other.clusterid != null)
        return false;
    } else if (!clusterid.equals(other.clusterid))
      return false;
    if (instanceid == null) {
      if (other.instanceid != null)
        return false;
    } else if (!instanceid.equals(other.instanceid))
      return false;
    if (starttime == null) {
      if (other.starttime != null)
        return false;
    } else if (!starttime.equals(other.starttime))
      return false;
    if (stoptime == null) {
      if (other.stoptime != null)
        return false;
    } else if (!stoptime.equals(other.stoptime))
      return false;
    if (parentnodeid == null) {
      if (other.parentnodeid != null)
        return false;
    } else if (!parentnodeid.equals(other.parentnodeid))
      return false;
    if (namespace == null) {
      if (other.namespace != null)
        return false;
    } else if (!namespace.equals(other.namespace))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (cpurequest == null) {
      if (other.cpurequest != null)
        return false;
    } else if (!cpurequest.equals(other.cpurequest))
      return false;
    if (memoryrequest == null) {
      if (other.memoryrequest != null)
        return false;
    } else if (!memoryrequest.equals(other.memoryrequest))
      return false;
    if (workloadid == null) {
      if (other.workloadid != null)
        return false;
    } else if (!workloadid.equals(other.workloadid))
      return false;
    if (createdat == null) {
      if (other.createdat != null)
        return false;
    } else if (!createdat.equals(other.createdat))
      return false;
    if (updatedat == null) {
      if (other.updatedat != null)
        return false;
    } else if (!updatedat.equals(other.updatedat))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.accountid == null) ? 0 : this.accountid.hashCode());
    result = prime * result + ((this.clusterid == null) ? 0 : this.clusterid.hashCode());
    result = prime * result + ((this.instanceid == null) ? 0 : this.instanceid.hashCode());
    result = prime * result + ((this.starttime == null) ? 0 : this.starttime.hashCode());
    result = prime * result + ((this.stoptime == null) ? 0 : this.stoptime.hashCode());
    result = prime * result + ((this.parentnodeid == null) ? 0 : this.parentnodeid.hashCode());
    result = prime * result + ((this.namespace == null) ? 0 : this.namespace.hashCode());
    result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
    result = prime * result + ((this.cpurequest == null) ? 0 : this.cpurequest.hashCode());
    result = prime * result + ((this.memoryrequest == null) ? 0 : this.memoryrequest.hashCode());
    result = prime * result + ((this.workloadid == null) ? 0 : this.workloadid.hashCode());
    result = prime * result + ((this.createdat == null) ? 0 : this.createdat.hashCode());
    result = prime * result + ((this.updatedat == null) ? 0 : this.updatedat.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("PodInfo (");

    sb.append(accountid);
    sb.append(", ").append(clusterid);
    sb.append(", ").append(instanceid);
    sb.append(", ").append(starttime);
    sb.append(", ").append(stoptime);
    sb.append(", ").append(parentnodeid);
    sb.append(", ").append(namespace);
    sb.append(", ").append(name);
    sb.append(", ").append(cpurequest);
    sb.append(", ").append(memoryrequest);
    sb.append(", ").append(workloadid);
    sb.append(", ").append(createdat);
    sb.append(", ").append(updatedat);

    sb.append(")");
    return sb.toString();
  }
}
