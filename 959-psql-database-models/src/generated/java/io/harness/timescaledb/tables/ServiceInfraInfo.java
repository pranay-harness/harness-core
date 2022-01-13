/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables;

import io.harness.timescaledb.Keys;
import io.harness.timescaledb.Public;
import io.harness.timescaledb.tables.records.ServiceInfraInfoRecord;

import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row16;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class ServiceInfraInfo extends TableImpl<ServiceInfraInfoRecord> {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public.service_infra_info</code>
   */
  public static final ServiceInfraInfo SERVICE_INFRA_INFO = new ServiceInfraInfo();

  /**
   * The class holding records for this type
   */
  @Override
  public Class<ServiceInfraInfoRecord> getRecordType() {
    return ServiceInfraInfoRecord.class;
  }

  /**
   * The column <code>public.service_infra_info.id</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> ID =
      createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.service_infra_info.service_name</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> SERVICE_NAME =
      createField(DSL.name("service_name"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.service_id</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> SERVICE_ID =
      createField(DSL.name("service_id"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.tag</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> TAG =
      createField(DSL.name("tag"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.env_name</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> ENV_NAME =
      createField(DSL.name("env_name"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.env_id</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> ENV_ID =
      createField(DSL.name("env_id"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.env_type</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> ENV_TYPE =
      createField(DSL.name("env_type"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.pipeline_execution_summary_cd_id</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> PIPELINE_EXECUTION_SUMMARY_CD_ID =
      createField(DSL.name("pipeline_execution_summary_cd_id"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.deployment_type</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> DEPLOYMENT_TYPE =
      createField(DSL.name("deployment_type"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.service_status</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> SERVICE_STATUS =
      createField(DSL.name("service_status"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.service_startts</code>.
   */
  public final TableField<ServiceInfraInfoRecord, Long> SERVICE_STARTTS =
      createField(DSL.name("service_startts"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.service_infra_info.service_endts</code>.
   */
  public final TableField<ServiceInfraInfoRecord, Long> SERVICE_ENDTS =
      createField(DSL.name("service_endts"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.service_infra_info.accountid</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> ACCOUNTID =
      createField(DSL.name("accountid"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.orgidentifier</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> ORGIDENTIFIER =
      createField(DSL.name("orgidentifier"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.projectidentifier</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> PROJECTIDENTIFIER =
      createField(DSL.name("projectidentifier"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.service_infra_info.artifact_image</code>.
   */
  public final TableField<ServiceInfraInfoRecord, String> ARTIFACT_IMAGE =
      createField(DSL.name("artifact_image"), SQLDataType.CLOB, this, "");

  private ServiceInfraInfo(Name alias, Table<ServiceInfraInfoRecord> aliased) {
    this(alias, aliased, null);
  }

  private ServiceInfraInfo(Name alias, Table<ServiceInfraInfoRecord> aliased, Field<?>[] parameters) {
    super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
  }

  /**
   * Create an aliased <code>public.service_infra_info</code> table reference
   */
  public ServiceInfraInfo(String alias) {
    this(DSL.name(alias), SERVICE_INFRA_INFO);
  }

  /**
   * Create an aliased <code>public.service_infra_info</code> table reference
   */
  public ServiceInfraInfo(Name alias) {
    this(alias, SERVICE_INFRA_INFO);
  }

  /**
   * Create a <code>public.service_infra_info</code> table reference
   */
  public ServiceInfraInfo() {
    this(DSL.name("service_infra_info"), null);
  }

  public <O extends Record> ServiceInfraInfo(Table<O> child, ForeignKey<O, ServiceInfraInfoRecord> key) {
    super(child, key, SERVICE_INFRA_INFO);
  }

  @Override
  public Schema getSchema() {
    return Public.PUBLIC;
  }

  @Override
  public UniqueKey<ServiceInfraInfoRecord> getPrimaryKey() {
    return Keys.SERVICE_INFRA_INFO_PKEY;
  }

  @Override
  public List<UniqueKey<ServiceInfraInfoRecord>> getKeys() {
    return Arrays.<UniqueKey<ServiceInfraInfoRecord>>asList(Keys.SERVICE_INFRA_INFO_PKEY);
  }

  @Override
  public ServiceInfraInfo as(String alias) {
    return new ServiceInfraInfo(DSL.name(alias), this);
  }

  @Override
  public ServiceInfraInfo as(Name alias) {
    return new ServiceInfraInfo(alias, this);
  }

  /**
   * Rename this table
   */
  @Override
  public ServiceInfraInfo rename(String name) {
    return new ServiceInfraInfo(DSL.name(name), null);
  }

  /**
   * Rename this table
   */
  @Override
  public ServiceInfraInfo rename(Name name) {
    return new ServiceInfraInfo(name, null);
  }

  // -------------------------------------------------------------------------
  // Row16 type methods
  // -------------------------------------------------------------------------

  @Override
  public Row16<String, String, String, String, String, String, String, String, String, String, Long, Long, String,
      String, String, String>
  fieldsRow() {
    return (Row16) super.fieldsRow();
  }
}
