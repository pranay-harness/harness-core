/*
 * This file is generated by jOOQ.
 */
package io.harness.timescaledb;

import io.harness.timescaledb.tables.Book;
import io.harness.timescaledb.tables.ProjectSummaryData;

import java.util.Arrays;
import java.util.List;
import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;

/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({"all", "unchecked", "rawtypes"})
public class Public extends SchemaImpl {
  private static final long serialVersionUID = 1L;

  /**
   * The reference instance of <code>public</code>
   */
  public static final Public PUBLIC = new Public();

  /**
   * The table <code>public.book</code>.
   */
  public final Book BOOK = Book.BOOK;

  /**
   * The table <code>public.project_summary_data</code>.
   */
  public final ProjectSummaryData PROJECT_SUMMARY_DATA = ProjectSummaryData.PROJECT_SUMMARY_DATA;

  /**
   * No further instances allowed
   */
  private Public() {
    super("public", null);
  }

  @Override
  public Catalog getCatalog() {
    return DefaultCatalog.DEFAULT_CATALOG;
  }

  @Override
  public final List<Table<?>> getTables() {
    return Arrays.<Table<?>>asList(Book.BOOK, ProjectSummaryData.PROJECT_SUMMARY_DATA);
  }
}
