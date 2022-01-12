/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables.records;

import io.harness.timescaledb.tables.ServiceInfraInfo;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record16;
import org.jooq.Row16;
import org.jooq.impl.UpdatableRecordImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class ServiceInfraInfoRecord
    extends UpdatableRecordImpl<ServiceInfraInfoRecord> implements Record16<String, String, String, String, String,
        String, String, String, String, String, Long, Long, String, String, String, String> {
  private static final long serialVersionUID = 1L;

  /**
   * Setter for <code>public.service_infra_info.id</code>.
   */
  public ServiceInfraInfoRecord setId(String value) {
    set(0, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.id</code>.
   */
  public String getId() {
    return (String) get(0);
  }

  /**
   * Setter for <code>public.service_infra_info.service_name</code>.
   */
  public ServiceInfraInfoRecord setServiceName(String value) {
    set(1, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_name</code>.
   */
  public String getServiceName() {
    return (String) get(1);
  }

  /**
   * Setter for <code>public.service_infra_info.service_id</code>.
   */
  public ServiceInfraInfoRecord setServiceId(String value) {
    set(2, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_id</code>.
   */
  public String getServiceId() {
    return (String) get(2);
  }

  /**
   * Setter for <code>public.service_infra_info.tag</code>.
   */
  public ServiceInfraInfoRecord setTag(String value) {
    set(3, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.tag</code>.
   */
  public String getTag() {
    return (String) get(3);
  }

  /**
   * Setter for <code>public.service_infra_info.env_name</code>.
   */
  public ServiceInfraInfoRecord setEnvName(String value) {
    set(4, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.env_name</code>.
   */
  public String getEnvName() {
    return (String) get(4);
  }

  /**
   * Setter for <code>public.service_infra_info.env_id</code>.
   */
  public ServiceInfraInfoRecord setEnvId(String value) {
    set(5, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.env_id</code>.
   */
  public String getEnvId() {
    return (String) get(5);
  }

  /**
   * Setter for <code>public.service_infra_info.env_type</code>.
   */
  public ServiceInfraInfoRecord setEnvType(String value) {
    set(6, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.env_type</code>.
   */
  public String getEnvType() {
    return (String) get(6);
  }

  /**
   * Setter for <code>public.service_infra_info.pipeline_execution_summary_cd_id</code>.
   */
  public ServiceInfraInfoRecord setPipelineExecutionSummaryCdId(String value) {
    set(7, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.pipeline_execution_summary_cd_id</code>.
   */
  public String getPipelineExecutionSummaryCdId() {
    return (String) get(7);
  }

  /**
   * Setter for <code>public.service_infra_info.deployment_type</code>.
   */
  public ServiceInfraInfoRecord setDeploymentType(String value) {
    set(8, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.deployment_type</code>.
   */
  public String getDeploymentType() {
    return (String) get(8);
  }

  /**
   * Setter for <code>public.service_infra_info.service_status</code>.
   */
  public ServiceInfraInfoRecord setServiceStatus(String value) {
    set(9, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_status</code>.
   */
  public String getServiceStatus() {
    return (String) get(9);
  }

  /**
   * Setter for <code>public.service_infra_info.service_startts</code>.
   */
  public ServiceInfraInfoRecord setServiceStartts(Long value) {
    set(10, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_startts</code>.
   */
  public Long getServiceStartts() {
    return (Long) get(10);
  }

  /**
   * Setter for <code>public.service_infra_info.service_endts</code>.
   */
  public ServiceInfraInfoRecord setServiceEndts(Long value) {
    set(11, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.service_endts</code>.
   */
  public Long getServiceEndts() {
    return (Long) get(11);
  }

  /**
   * Setter for <code>public.service_infra_info.accountid</code>.
   */
  public ServiceInfraInfoRecord setAccountid(String value) {
    set(12, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.accountid</code>.
   */
  public String getAccountid() {
    return (String) get(12);
  }

  /**
   * Setter for <code>public.service_infra_info.orgidentifier</code>.
   */
  public ServiceInfraInfoRecord setOrgidentifier(String value) {
    set(13, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.orgidentifier</code>.
   */
  public String getOrgidentifier() {
    return (String) get(13);
  }

  /**
   * Setter for <code>public.service_infra_info.projectidentifier</code>.
   */
  public ServiceInfraInfoRecord setProjectidentifier(String value) {
    set(14, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.projectidentifier</code>.
   */
  public String getProjectidentifier() {
    return (String) get(14);
  }

  /**
   * Setter for <code>public.service_infra_info.artifact_image</code>.
   */
  public ServiceInfraInfoRecord setArtifactImage(String value) {
    set(15, value);
    return this;
  }

  /**
   * Getter for <code>public.service_infra_info.artifact_image</code>.
   */
  public String getArtifactImage() {
    return (String) get(15);
  }

  // -------------------------------------------------------------------------
  // Primary key information
  // -------------------------------------------------------------------------

  @Override
  public Record1<String> key() {
    return (Record1) super.key();
  }

  // -------------------------------------------------------------------------
  // Record16 type implementation
  // -------------------------------------------------------------------------

  @Override
  public Row16<String, String, String, String, String, String, String, String, String, String, Long, Long, String,
      String, String, String>
  fieldsRow() {
    return (Row16) super.fieldsRow();
  }

  @Override
  public Row16<String, String, String, String, String, String, String, String, String, String, Long, Long, String,
      String, String, String>
  valuesRow() {
    return (Row16) super.valuesRow();
  }

  @Override
  public Field<String> field1() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.ID;
  }

  @Override
  public Field<String> field2() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.SERVICE_NAME;
  }

  @Override
  public Field<String> field3() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.SERVICE_ID;
  }

  @Override
  public Field<String> field4() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.TAG;
  }

  @Override
  public Field<String> field5() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.ENV_NAME;
  }

  @Override
  public Field<String> field6() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.ENV_ID;
  }

  @Override
  public Field<String> field7() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.ENV_TYPE;
  }

  @Override
  public Field<String> field8() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.PIPELINE_EXECUTION_SUMMARY_CD_ID;
  }

  @Override
  public Field<String> field9() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.DEPLOYMENT_TYPE;
  }

  @Override
  public Field<String> field10() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.SERVICE_STATUS;
  }

  @Override
  public Field<Long> field11() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.SERVICE_STARTTS;
  }

  @Override
  public Field<Long> field12() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.SERVICE_ENDTS;
  }

  @Override
  public Field<String> field13() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.ACCOUNTID;
  }

  @Override
  public Field<String> field14() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.ORGIDENTIFIER;
  }

  @Override
  public Field<String> field15() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.PROJECTIDENTIFIER;
  }

  @Override
  public Field<String> field16() {
    return ServiceInfraInfo.SERVICE_INFRA_INFO.ARTIFACT_IMAGE;
  }

  @Override
  public String component1() {
    return getId();
  }

  @Override
  public String component2() {
    return getServiceName();
  }

  @Override
  public String component3() {
    return getServiceId();
  }

  @Override
  public String component4() {
    return getTag();
  }

  @Override
  public String component5() {
    return getEnvName();
  }

  @Override
  public String component6() {
    return getEnvId();
  }

  @Override
  public String component7() {
    return getEnvType();
  }

  @Override
  public String component8() {
    return getPipelineExecutionSummaryCdId();
  }

  @Override
  public String component9() {
    return getDeploymentType();
  }

  @Override
  public String component10() {
    return getServiceStatus();
  }

  @Override
  public Long component11() {
    return getServiceStartts();
  }

  @Override
  public Long component12() {
    return getServiceEndts();
  }

  @Override
  public String component13() {
    return getAccountid();
  }

  @Override
  public String component14() {
    return getOrgidentifier();
  }

  @Override
  public String component15() {
    return getProjectidentifier();
  }

  @Override
  public String component16() {
    return getArtifactImage();
  }

  @Override
  public String value1() {
    return getId();
  }

  @Override
  public String value2() {
    return getServiceName();
  }

  @Override
  public String value3() {
    return getServiceId();
  }

  @Override
  public String value4() {
    return getTag();
  }

  @Override
  public String value5() {
    return getEnvName();
  }

  @Override
  public String value6() {
    return getEnvId();
  }

  @Override
  public String value7() {
    return getEnvType();
  }

  @Override
  public String value8() {
    return getPipelineExecutionSummaryCdId();
  }

  @Override
  public String value9() {
    return getDeploymentType();
  }

  @Override
  public String value10() {
    return getServiceStatus();
  }

  @Override
  public Long value11() {
    return getServiceStartts();
  }

  @Override
  public Long value12() {
    return getServiceEndts();
  }

  @Override
  public String value13() {
    return getAccountid();
  }

  @Override
  public String value14() {
    return getOrgidentifier();
  }

  @Override
  public String value15() {
    return getProjectidentifier();
  }

  @Override
  public String value16() {
    return getArtifactImage();
  }

  @Override
  public ServiceInfraInfoRecord value1(String value) {
    setId(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value2(String value) {
    setServiceName(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value3(String value) {
    setServiceId(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value4(String value) {
    setTag(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value5(String value) {
    setEnvName(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value6(String value) {
    setEnvId(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value7(String value) {
    setEnvType(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value8(String value) {
    setPipelineExecutionSummaryCdId(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value9(String value) {
    setDeploymentType(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value10(String value) {
    setServiceStatus(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value11(Long value) {
    setServiceStartts(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value12(Long value) {
    setServiceEndts(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value13(String value) {
    setAccountid(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value14(String value) {
    setOrgidentifier(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value15(String value) {
    setProjectidentifier(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord value16(String value) {
    setArtifactImage(value);
    return this;
  }

  @Override
  public ServiceInfraInfoRecord values(String value1, String value2, String value3, String value4, String value5,
      String value6, String value7, String value8, String value9, String value10, Long value11, Long value12,
      String value13, String value14, String value15, String value16) {
    value1(value1);
    value2(value2);
    value3(value3);
    value4(value4);
    value5(value5);
    value6(value6);
    value7(value7);
    value8(value8);
    value9(value9);
    value10(value10);
    value11(value11);
    value12(value12);
    value13(value13);
    value14(value14);
    value15(value15);
    value16(value16);
    return this;
  }

  // -------------------------------------------------------------------------
  // Constructors
  // -------------------------------------------------------------------------

  /**
   * Create a detached ServiceInfraInfoRecord
   */
  public ServiceInfraInfoRecord() {
    super(ServiceInfraInfo.SERVICE_INFRA_INFO);
  }

  /**
   * Create a detached, initialised ServiceInfraInfoRecord
   */
  public ServiceInfraInfoRecord(String id, String serviceName, String serviceId, String tag, String envName,
      String envId, String envType, String pipelineExecutionSummaryCdId, String deploymentType, String serviceStatus,
      Long serviceStartts, Long serviceEndts, String accountid, String orgidentifier, String projectidentifier,
      String artifactImage) {
    super(ServiceInfraInfo.SERVICE_INFRA_INFO);

    setId(id);
    setServiceName(serviceName);
    setServiceId(serviceId);
    setTag(tag);
    setEnvName(envName);
    setEnvId(envId);
    setEnvType(envType);
    setPipelineExecutionSummaryCdId(pipelineExecutionSummaryCdId);
    setDeploymentType(deploymentType);
    setServiceStatus(serviceStatus);
    setServiceStartts(serviceStartts);
    setServiceEndts(serviceEndts);
    setAccountid(accountid);
    setOrgidentifier(orgidentifier);
    setProjectidentifier(projectidentifier);
    setArtifactImage(artifactImage);
  }
}
