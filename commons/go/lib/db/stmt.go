// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package db

import (
	"context"
	"database/sql"
	"time"

	"go.uber.org/zap"
)

// Stmt wraps an *sql.Stmt with logging
type Stmt struct {
	base  *sql.Stmt
	query string
	log   *zap.SugaredLogger
	db    *DB
}

func newStmt(base *sql.Stmt, query string, db *DB, log *zap.SugaredLogger) *Stmt {
	return &Stmt{
		base:  base,
		query: query,
		log:   log,
		db:    db,
	}
}

//Exec runs the given sql statement , logs details about the call and returns the result
func (s Stmt) Exec(args ...interface{}) (sql.Result, error) {
	start := time.Now()
	res, err := s.base.Exec(args...)
	logQuery(s.log, start, s.query, args, err)
	return res, err
}

//Query runs the given sql statement, logs details about the call and returns the result
func (s Stmt) Query(args ...interface{}) (*sql.Rows, error) {
	start := time.Now()
	res, err := s.base.Query(args...)
	logQuery(s.log, start, s.query, args, err)
	return res, err
}

//QueryRow runs the given sql statement, logs details about the call and returns the result
func (s Stmt) QueryRow(args ...interface{}) *sql.Row {
	start := time.Now()
	row := s.base.QueryRow(args...)
	logQuery(s.log, start, s.query, args, nil)
	return row
}

//ExecContext runs the given sql statement using the given context,
//logs details about the call and returns the result
func (s Stmt) ExecContext(ctx context.Context, args ...interface{}) (sql.Result, error) {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "stmt.base.ExecContext", s.db.ci.Application, s.db.ci.DBName, s.db.ci.Engine, s.query)
	defer deferFunc()
	res, err := s.base.ExecContext(ctx, args...)
	logQuery(s.log, start, s.query, args, err)
	return res, err
}

// QueryContext runs the given sql statement using the given context,
// logs details about the call and returns the result
func (s Stmt) QueryContext(ctx context.Context, args ...interface{}) (*sql.Rows, error) {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "stmt.base.QueryContext", s.db.ci.Application, s.db.ci.DBName, s.db.ci.Engine, s.query)
	defer deferFunc()
	rows, err := s.base.QueryContext(ctx, args...)
	logQuery(s.log, start, s.query, args, err)
	return rows, err
}

// QueryRowContext runs the given sql statement using the given context,
// logs details about the call and returns the result
func (s Stmt) QueryRowContext(ctx context.Context, args ...interface{}) *sql.Row {
	start := time.Now()
	deferFunc := startNewSpanIfContextHasSpan(ctx, "tx.base.QueryRowContext", s.db.ci.Application, s.db.ci.DBName, s.db.ci.Engine, s.query)
	defer deferFunc()
	row := s.base.QueryRowContext(ctx, args...)
	logQuery(s.log, start, s.query, args, nil)
	return row
}

//Close closes the base statement
func (s *Stmt) Close() error {
	return s.base.Close()
}
