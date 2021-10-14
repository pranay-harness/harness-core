/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb.tables;

import io.harness.timescaledb.Indexes;
import io.harness.timescaledb.Public;
import io.harness.timescaledb.tables.records.EnvironmentsRecord;

import java.util.Arrays;
import java.util.List;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row9;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Environments extends TableImpl<EnvironmentsRecord> {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public.environments</code>
   */
  public static final Environments ENVIRONMENTS = new Environments();

  /**
   * The class holding records for this type
   */
  @Override
  public Class<EnvironmentsRecord> getRecordType() {
    return EnvironmentsRecord.class;
  }

  /**
   * The column <code>public.environments.id</code>.
   */
  public final TableField<EnvironmentsRecord, String> ID =
      createField(DSL.name("id"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.environments.account_id</code>.
   */
  public final TableField<EnvironmentsRecord, String> ACCOUNT_ID =
      createField(DSL.name("account_id"), SQLDataType.CLOB.nullable(false), this, "");

  /**
   * The column <code>public.environments.org_identifier</code>.
   */
  public final TableField<EnvironmentsRecord, String> ORG_IDENTIFIER =
      createField(DSL.name("org_identifier"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.environments.project_identifier</code>.
   */
  public final TableField<EnvironmentsRecord, String> PROJECT_IDENTIFIER =
      createField(DSL.name("project_identifier"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.environments.identifier</code>.
   */
  public final TableField<EnvironmentsRecord, String> IDENTIFIER =
      createField(DSL.name("identifier"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.environments.name</code>.
   */
  public final TableField<EnvironmentsRecord, String> NAME = createField(DSL.name("name"), SQLDataType.CLOB, this, "");

  /**
   * The column <code>public.environments.deleted</code>.
   */
  public final TableField<EnvironmentsRecord, Boolean> DELETED =
      createField(DSL.name("deleted"), SQLDataType.BOOLEAN, this, "");

  /**
   * The column <code>public.environments.created_at</code>.
   */
  public final TableField<EnvironmentsRecord, Long> CREATED_AT =
      createField(DSL.name("created_at"), SQLDataType.BIGINT, this, "");

  /**
   * The column <code>public.environments.last_modified_at</code>.
   */
  public final TableField<EnvironmentsRecord, Long> LAST_MODIFIED_AT =
      createField(DSL.name("last_modified_at"), SQLDataType.BIGINT, this, "");

  private Environments(Name alias, Table<EnvironmentsRecord> aliased) {
    this(alias, aliased, null);
  }

  private Environments(Name alias, Table<EnvironmentsRecord> aliased, Field<?>[] parameters) {
    super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table());
  }

  /**
   * Create an aliased <code>public.environments</code> table reference
   */
  public Environments(String alias) {
    this(DSL.name(alias), ENVIRONMENTS);
  }

  /**
   * Create an aliased <code>public.environments</code> table reference
   */
  public Environments(Name alias) {
    this(alias, ENVIRONMENTS);
  }

  /**
   * Create a <code>public.environments</code> table reference
   */
  public Environments() {
    this(DSL.name("environments"), null);
  }

  public <O extends Record> Environments(Table<O> child, ForeignKey<O, EnvironmentsRecord> key) {
    super(child, key, ENVIRONMENTS);
  }

  @Override
  public Schema getSchema() {
    return Public.PUBLIC;
  }

  @Override
  public List<Index> getIndexes() {
    return Arrays.<Index>asList(Indexes.ENVIRONMENTS_ACCOUNT_ID_CREATED_AT_IDX);
  }

  @Override
  public Environments as(String alias) {
    return new Environments(DSL.name(alias), this);
  }

  @Override
  public Environments as(Name alias) {
    return new Environments(alias, this);
  }

  /**
   * Rename this table
   */
  @Override
  public Environments rename(String name) {
    return new Environments(DSL.name(name), null);
  }

  /**
   * Rename this table
   */
  @Override
  public Environments rename(Name name) {
    return new Environments(name, null);
  }

  // -------------------------------------------------------------------------
  // Row9 type methods
  // -------------------------------------------------------------------------

  @Override
  public Row9<String, String, String, String, String, String, Boolean, Long, Long> fieldsRow() {
    return (Row9) super.fieldsRow();
  }
}
