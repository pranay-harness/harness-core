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
public class OverviewDashboard implements Serializable {
  private static final long serialVersionUID = 1L;

  private OffsetDateTime starttime;
  private OffsetDateTime endtime;
  private String accountid;
  private String settingid;
  private String instanceid;
  private String instancetype;
  private Double cpu;
  private Double memory;

  public OverviewDashboard() {}

  public OverviewDashboard(OverviewDashboard value) {
    this.starttime = value.starttime;
    this.endtime = value.endtime;
    this.accountid = value.accountid;
    this.settingid = value.settingid;
    this.instanceid = value.instanceid;
    this.instancetype = value.instancetype;
    this.cpu = value.cpu;
    this.memory = value.memory;
  }

  public OverviewDashboard(OffsetDateTime starttime, OffsetDateTime endtime, String accountid, String settingid,
      String instanceid, String instancetype, Double cpu, Double memory) {
    this.starttime = starttime;
    this.endtime = endtime;
    this.accountid = accountid;
    this.settingid = settingid;
    this.instanceid = instanceid;
    this.instancetype = instancetype;
    this.cpu = cpu;
    this.memory = memory;
  }

  /**
   * Getter for <code>public.overview_dashboard.starttime</code>.
   */
  public OffsetDateTime getStarttime() {
    return this.starttime;
  }

  /**
   * Setter for <code>public.overview_dashboard.starttime</code>.
   */
  public OverviewDashboard setStarttime(OffsetDateTime starttime) {
    this.starttime = starttime;
    return this;
  }

  /**
   * Getter for <code>public.overview_dashboard.endtime</code>.
   */
  public OffsetDateTime getEndtime() {
    return this.endtime;
  }

  /**
   * Setter for <code>public.overview_dashboard.endtime</code>.
   */
  public OverviewDashboard setEndtime(OffsetDateTime endtime) {
    this.endtime = endtime;
    return this;
  }

  /**
   * Getter for <code>public.overview_dashboard.accountid</code>.
   */
  public String getAccountid() {
    return this.accountid;
  }

  /**
   * Setter for <code>public.overview_dashboard.accountid</code>.
   */
  public OverviewDashboard setAccountid(String accountid) {
    this.accountid = accountid;
    return this;
  }

  /**
   * Getter for <code>public.overview_dashboard.settingid</code>.
   */
  public String getSettingid() {
    return this.settingid;
  }

  /**
   * Setter for <code>public.overview_dashboard.settingid</code>.
   */
  public OverviewDashboard setSettingid(String settingid) {
    this.settingid = settingid;
    return this;
  }

  /**
   * Getter for <code>public.overview_dashboard.instanceid</code>.
   */
  public String getInstanceid() {
    return this.instanceid;
  }

  /**
   * Setter for <code>public.overview_dashboard.instanceid</code>.
   */
  public OverviewDashboard setInstanceid(String instanceid) {
    this.instanceid = instanceid;
    return this;
  }

  /**
   * Getter for <code>public.overview_dashboard.instancetype</code>.
   */
  public String getInstancetype() {
    return this.instancetype;
  }

  /**
   * Setter for <code>public.overview_dashboard.instancetype</code>.
   */
  public OverviewDashboard setInstancetype(String instancetype) {
    this.instancetype = instancetype;
    return this;
  }

  /**
   * Getter for <code>public.overview_dashboard.cpu</code>.
   */
  public Double getCpu() {
    return this.cpu;
  }

  /**
   * Setter for <code>public.overview_dashboard.cpu</code>.
   */
  public OverviewDashboard setCpu(Double cpu) {
    this.cpu = cpu;
    return this;
  }

  /**
   * Getter for <code>public.overview_dashboard.memory</code>.
   */
  public Double getMemory() {
    return this.memory;
  }

  /**
   * Setter for <code>public.overview_dashboard.memory</code>.
   */
  public OverviewDashboard setMemory(Double memory) {
    this.memory = memory;
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
    final OverviewDashboard other = (OverviewDashboard) obj;
    if (starttime == null) {
      if (other.starttime != null)
        return false;
    } else if (!starttime.equals(other.starttime))
      return false;
    if (endtime == null) {
      if (other.endtime != null)
        return false;
    } else if (!endtime.equals(other.endtime))
      return false;
    if (accountid == null) {
      if (other.accountid != null)
        return false;
    } else if (!accountid.equals(other.accountid))
      return false;
    if (settingid == null) {
      if (other.settingid != null)
        return false;
    } else if (!settingid.equals(other.settingid))
      return false;
    if (instanceid == null) {
      if (other.instanceid != null)
        return false;
    } else if (!instanceid.equals(other.instanceid))
      return false;
    if (instancetype == null) {
      if (other.instancetype != null)
        return false;
    } else if (!instancetype.equals(other.instancetype))
      return false;
    if (cpu == null) {
      if (other.cpu != null)
        return false;
    } else if (!cpu.equals(other.cpu))
      return false;
    if (memory == null) {
      if (other.memory != null)
        return false;
    } else if (!memory.equals(other.memory))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.starttime == null) ? 0 : this.starttime.hashCode());
    result = prime * result + ((this.endtime == null) ? 0 : this.endtime.hashCode());
    result = prime * result + ((this.accountid == null) ? 0 : this.accountid.hashCode());
    result = prime * result + ((this.settingid == null) ? 0 : this.settingid.hashCode());
    result = prime * result + ((this.instanceid == null) ? 0 : this.instanceid.hashCode());
    result = prime * result + ((this.instancetype == null) ? 0 : this.instancetype.hashCode());
    result = prime * result + ((this.cpu == null) ? 0 : this.cpu.hashCode());
    result = prime * result + ((this.memory == null) ? 0 : this.memory.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("OverviewDashboard (");

    sb.append(starttime);
    sb.append(", ").append(endtime);
    sb.append(", ").append(accountid);
    sb.append(", ").append(settingid);
    sb.append(", ").append(instanceid);
    sb.append(", ").append(instancetype);
    sb.append(", ").append(cpu);
    sb.append(", ").append(memory);

    sb.append(")");
    return sb.toString();
  }
}
