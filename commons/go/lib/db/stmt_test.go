// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

package db

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
	"go.uber.org/zap/zaptest/observer"
	"gopkg.in/DATA-DOG/go-sqlmock.v1"
)

func Test_StmtExec(t *testing.T) {
	const (
		updateSQL = "UPDATE table SET a = $1 WHERE b = $2;"
	)

	core, logs := observer.New(zapcore.InfoLevel)
	log := zap.New(core).Sugar()

	db, mock, err := NewMockDB(log)
	require.NoError(t, err, "No error expected when opening stub database connection")

	mock.MatchExpectationsInOrder(true)
	mock.ExpectPrepare("UPDATE table")
	mock.ExpectExec("UPDATE table").WithArgs("a", "b").WillReturnResult(sqlmock.NewResult(1, 1))

	stmt, err := db.Prepare(updateSQL)
	assert.NoError(t, err, "prepare shouldn't error")

	_, err = stmt.Exec("a", "b")
	assert.NoError(t, err, "exec shouldn't error")

	assert.NoError(t, mock.ExpectationsWereMet())
	assert.Len(t, logs.TakeAll(), 2)
}

func Test_Stmt(t *testing.T) {
	const (
		querySQL    = "SELECT a FROM table;"
		updateSQL   = "UPDATE table SET a = $1 WHERE b = $2;"
		queryRowSQL = "SELECT a FROM table WHERE b = $1;"
	)

	core, logs := observer.New(zapcore.InfoLevel)
	log := zap.New(core).Sugar()
	ctx := context.Background()

	db, mock, err := NewMockDB(log)
	require.NoError(t, err, "No error expected when opening stub database connection")
	mock.MatchExpectationsInOrder(true)

	// update Exec statement
	mock.ExpectPrepare("UPDATE table")
	mock.ExpectExec("UPDATE table").WithArgs("a", "b").WillReturnResult(sqlmock.NewResult(1, 1))
	stmt, err := db.Prepare(updateSQL)
	assert.NoError(t, err, "prepare shouldn't error")

	_, err = stmt.Exec("a", "b")
	assert.NoError(t, err, "exec shouldn't error")

	// query statement
	mock.ExpectPrepare(querySQL)
	mock.ExpectQuery(querySQL).WillReturnRows(sqlmock.NewRows([]string{"a"}).AddRow("a"))
	stmt, err = db.Prepare(querySQL)
	assert.NoError(t, err, "prepare shouldn't error")

	_, err = stmt.Query()
	assert.NoError(t, err, "query shouldn't error")

	// query row statement
	mock.ExpectPrepare("SELECT a FROM table WHERE")
	mock.ExpectQuery("SELECT a FROM table WHERE b =").WithArgs("b").WillReturnRows(sqlmock.NewRows([]string{"a"}).AddRow("a"))
	stmt, err = db.Prepare(queryRowSQL)
	assert.NoError(t, err, "prepare shouldn't error")

	_ = stmt.QueryRow("b")
	assert.NoError(t, err, "query shouldn't error")

	// update Exec statement with context
	mock.ExpectPrepare("UPDATE table")
	mock.ExpectExec("UPDATE table").WithArgs("a", "b").WillReturnResult(sqlmock.NewResult(1, 1))
	stmt, err = db.Prepare(updateSQL)
	assert.NoError(t, err, "prepare shouldn't error")

	_, err = stmt.ExecContext(ctx, "a", "b")
	assert.NoError(t, err, "exec shouldn't error")

	// query statement with context
	mock.ExpectPrepare(querySQL)
	mock.ExpectQuery(querySQL).WillReturnRows(sqlmock.NewRows([]string{"a"}).AddRow("a"))
	stmt, err = db.Prepare(querySQL)
	assert.NoError(t, err, "prepare shouldn't error")

	_, err = stmt.QueryContext(ctx)
	assert.NoError(t, err, "query shouldn't error")

	// query row statement with context
	mock.ExpectPrepare("SELECT a FROM table WHERE")
	mock.ExpectQuery("SELECT a FROM table WHERE b =").WithArgs("b").WillReturnRows(sqlmock.NewRows([]string{"a"}).AddRow("a"))
	stmt, err = db.Prepare(queryRowSQL)
	assert.NoError(t, err, "prepare shouldn't error")

	_ = stmt.QueryRowContext(ctx, "b")
	assert.NoError(t, err, "query shouldn't error")

	assert.NoError(t, mock.ExpectationsWereMet())
	assert.Len(t, logs.TakeAll(), 12)
}
