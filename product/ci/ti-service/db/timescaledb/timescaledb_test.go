// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package timescaledb

import (
	"context"
	"fmt"
	"regexp"
	"strconv"
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	"github.com/wings-software/portal/commons/go/lib/db"
	"github.com/wings-software/portal/product/ci/ti-service/logger"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
	"gopkg.in/DATA-DOG/go-sqlmock.v1"
)

func Test_SingleWrite(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	stage := "stage"
	step := "step"
	report := "junit"
	repo := "repo"
	sha := "sha"

	tn := time.Now()

	oldNow := now
	defer func() { now = oldNow }()
	now = func() time.Time { return tn }

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	valueStrings := constructPsqlInsertStmt(1, 21)
	stmt := fmt.Sprintf(
		`
				INSERT INTO %s
				(created_at, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, report, repo, commit_id,
				name, suite_name, class_name, duration_ms, result, message, type, description, stdout, stderr)
				VALUES %s`, table, valueStrings)
	stmt = regexp.QuoteMeta(stmt)
	mock.ExpectExec(stmt).
		WithArgs(tn, account, org, project,
			pipeline, build, stage, step, report, repo, sha, "blah", "", "", 0, "", "", "", "", "", "").WillReturnResult(sqlmock.NewResult(0, 1))
	tdb := &TimeScaleDb{Conn: db, EvalTable: table}
	test1 := &types.TestCase{Name: "blah"}
	err = tdb.Write(ctx, account, org, project, pipeline, build, stage, step, report, repo, sha, test1)
	assert.Nil(t, err, nil)
}

func Test_Write_Batch(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	// Set write batch size of 2 and try to write 3 tests.
	writeBatchSize = 2

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	stage := "stage"
	step := "step"
	report := "junit"
	repo := "repo"
	sha := "sha"

	tn := time.Now()

	oldNow := now
	defer func() { now = oldNow }()
	now = func() time.Time { return tn }

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	valueStrings := constructPsqlInsertStmt(1, 21)
	stmt := fmt.Sprintf(
		`
				INSERT INTO %s
				(created_at, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, report, repo, commit_id,
				name, suite_name, class_name, duration_ms, result, message, type, description, stdout, stderr)
				VALUES %s`, table, valueStrings)
	stmt = regexp.QuoteMeta(stmt)
	// First batched statement
	mock.ExpectExec(stmt).
		WithArgs(tn, account, org, project, pipeline, build, stage, step, report, repo, sha, "test1", "suite1", "class1", 10, "passed", "msg1", "type1", "desc1", "out1", "err1",
			tn, account, org, project, pipeline, build, stage, step, report, repo, sha, "test2", "suite2", "class2", 11, "failed", "msg2", "type2", "desc2", "out2", "err2").WillReturnResult(sqlmock.NewResult(0, 2))
	// Leftover writes
	mock.ExpectExec(stmt).
		WithArgs(tn, account, org, project, pipeline, build, stage, step, report, repo, sha, "test3", "suite3", "class3", 12, "error", "msg3", "type3", "desc3", "out3", "err3").WillReturnResult(sqlmock.NewResult(0, 1))
	test1 := &types.TestCase{Name: "test1", ClassName: "class1", SuiteName: "suite1",
		SystemOut: "out1", SystemErr: "err1", DurationMs: 10,
		Result: types.Result{Status: types.StatusPassed, Message: "msg1", Type: "type1", Desc: "desc1"}}
	test2 := &types.TestCase{Name: "test2", ClassName: "class2", SuiteName: "suite2",
		SystemOut: "out2", SystemErr: "err2", DurationMs: 11,
		Result: types.Result{Status: types.StatusFailed, Message: "msg2", Type: "type2", Desc: "desc2"}}
	test3 := &types.TestCase{Name: "test3", ClassName: "class3", SuiteName: "suite3",
		SystemOut: "out3", SystemErr: "err3", DurationMs: 12,
		Result: types.Result{Status: types.StatusError, Message: "msg3", Type: "type3", Desc: "desc3"}}
	tdb := &TimeScaleDb{Conn: db, EvalTable: table}
	err = tdb.Write(ctx, account, org, project, pipeline, build, stage, step, report, repo, sha, test1, test2, test3)
	assert.Nil(t, err, nil)
}

func Test_Summary(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	report := "junit"
	step := "step"
	stage := "stage"

	log := zap.NewExample().Sugar()
	logger.InitLogger(log)
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	col := []string{"duration_ms", "status", "name"}
	rows := sqlmock.NewRows(col).
		AddRow(10, "failed", "t1").
		AddRow(25, "passed", "t2")
	query := fmt.Sprintf(`
		SELECT duration_ms, result, name FROM %s WHERE account_id = $1
		AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND step_id = $6 AND stage_id = $7 AND report = $8;`, table)
	t1 := types.TestSummary{Name: "t1", Status: types.StatusFailed}
	t2 := types.TestSummary{Name: "t2", Status: types.StatusPassed}
	summary := []types.TestSummary{t1, t2}
	exp := types.SummaryResponse{
		TotalTests: 2,
		TimeMs:     35,
		Tests:      summary,
	}
	query = regexp.QuoteMeta(query)
	mock.ExpectQuery(query).
		WithArgs(account, org, project, pipeline, build, step, stage, report).WillReturnRows(rows)
	tdb := &TimeScaleDb{Conn: db, EvalTable: table}
	got, err := tdb.Summary(ctx, account, org, project, pipeline, build, step, stage, report)
	assert.Nil(t, err, nil)
	assert.Equal(t, got.TotalTests, exp.TotalTests)
	assert.Equal(t, got.TimeMs, exp.TimeMs)
	assert.ElementsMatch(t, got.Tests, exp.Tests)
}

func Test_GetTestCases(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	report := "junit"
	suite := "s1"
	step := "step"
	stage := "stage"

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	col := []string{"name", "suite_name", "class_name", "duration_ms", "status", "message", "description", "type", "stdout",
		"stderr", "full_count"}
	rows := sqlmock.NewRows(col).
		AddRow("t1", suite, "c1", 10, "failed", "m1", "d1", "type1", "o1", "e1", 2).
		AddRow("t2", suite, "c2", 5, "error", "m2", "d2", "type2", "o2", "e2", 2)
	tc1 := types.TestCase{
		Name:      "t1",
		ClassName: "c1",
		SuiteName: suite,
		Result: types.Result{
			Status:  types.StatusFailed,
			Message: "m1",
			Type:    "type1",
			Desc:    "d1",
		},
		DurationMs: 10,
		SystemOut:  "o1",
		SystemErr:  "e1",
	}
	tc2 := types.TestCase{
		Name:      "t2",
		ClassName: "c2",
		SuiteName: suite,
		Result: types.Result{
			Status:  types.StatusError,
			Message: "m2",
			Type:    "type2",
			Desc:    "d2",
		},
		DurationMs: 5,
		SystemOut:  "o2",
		SystemErr:  "e2",
	}
	tests := []types.TestCase{tc1, tc2}
	query := fmt.Sprintf(
		`
		SELECT name, suite_name, class_name, duration_ms, result, message,
		description, type, stdout, stderr, COUNT(*) OVER() AS full_count
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND step_id = $6 AND stage_id = $7 AND report = $8 AND suite_name = $9 AND result IN (%s)
		ORDER BY %s %s, %s %s
		LIMIT $10 OFFSET $11;`, table, "'failed', 'error'", "duration_ms", "DESC", "name", "ASC")
	query = regexp.QuoteMeta(query)
	mock.ExpectQuery(query).
		WithArgs(account, org, project, pipeline, build, step, stage, report, suite, defaultLimit, defaultOffset).WillReturnRows(rows)
	tdb := &TimeScaleDb{Conn: db, EvalTable: table}
	got, err := tdb.GetTestCases(ctx, account, org, project, pipeline, build, step, stage, report, suite, "duration_ms", "failed", desc, "", "")
	fmt.Println("\n\ngot: ", got)
	li, _ := strconv.Atoi(defaultLimit)
	assert.Nil(t, err, nil)
	assert.Equal(t, got.Metadata.TotalPages, 1)
	assert.Equal(t, got.Metadata.TotalItems, 2)
	assert.Equal(t, got.Metadata.PageItemCount, 2)
	assert.Equal(t, got.Metadata.PageSize, li)
	assert.ElementsMatch(t, got.Tests, tests)
}

func Test_GetTestSuites(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	step := "step"
	stage := "stage"
	report := "junit"

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	col := []string{"suite_name", "duration_ms", "total_tests", "skipped_tests", "passed_tests", "failed_tests", "fail_pct", "full_count"}
	rows := sqlmock.NewRows(col).
		AddRow("s1", 10, 4, 1, 1, 2, 50, 2).
		AddRow("s2", 5, 2, 1, 1, 0, 0, 2)
	ts1 := types.TestSuite{
		Name:         "s1",
		DurationMs:   10,
		TotalTests:   4,
		FailedTests:  2,
		SkippedTests: 1,
		PassedTests:  1,
		FailPct:      50,
	}
	ts2 := types.TestSuite{
		Name:         "s2",
		DurationMs:   5,
		TotalTests:   2,
		FailedTests:  0,
		SkippedTests: 1,
		PassedTests:  1,
		FailPct:      0,
	}
	suites := []types.TestSuite{ts1, ts2}
	query := fmt.Sprintf(
		`
		SELECT suite_name, SUM(duration_ms) AS duration_ms, COUNT(*) AS total_tests,
		SUM(CASE WHEN result = 'skipped' THEN 1 ELSE 0 END) AS skipped_tests,
		SUM(CASE WHEN result = 'passed' THEN 1 ELSE 0 END) AS passed_tests,
		SUM(CASE WHEN result = 'failed' OR result = 'error' THEN 1 ELSE 0 END) AS failed_tests,
		SUM(CASE WHEN result = 'failed' OR result = 'error' THEN 1 ELSE 0 END) * 100 / COUNT(*) AS fail_pct,
		COUNT(*) OVER() AS full_count
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND step_id = $6 AND stage_id = $7 AND report = $8 AND result IN (%s)
		GROUP BY suite_name
		ORDER BY %s %s, %s %s
		LIMIT $9 OFFSET $10;`, table, "'failed', 'error', 'passed', 'skipped'", "fail_pct", desc, "suite_name", asc)
	query = regexp.QuoteMeta(query)
	mock.ExpectQuery(query).
		WithArgs(account, org, project, pipeline, build, step, stage, report, defaultLimit, defaultOffset).WillReturnRows(rows)
	tdb := &TimeScaleDb{Conn: db, EvalTable: table}
	got, err := tdb.GetTestSuites(ctx, account, org, project, pipeline, build, step, stage, report, "", "", "", "", "")
	li, _ := strconv.Atoi(defaultLimit)
	assert.Nil(t, err, nil)
	assert.Equal(t, got.Metadata.TotalPages, 1)
	assert.Equal(t, got.Metadata.TotalItems, 2)
	assert.Equal(t, got.Metadata.PageItemCount, 2)
	assert.Equal(t, got.Metadata.PageSize, li)
	assert.ElementsMatch(t, got.Suites, suites)
}

func Test_WriteSelectedTests(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	stage := "stage"
	step := "step"
	repo := "repo"
	source := "source"
	target := "target"

	total := 20
	selected := 10
	new := 5
	updated := 3
	src := 2

	arg := types.SelectTestsResp{
		TotalTests:    total,
		SelectedTests: selected,
		NewTests:      new,
		UpdatedTests:  updated,
		SrcCodeTests:  src,
		SelectAll:     false,
		Tests:         nil,
	}

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}
	valueStrings := constructPsqlInsertStmt(1, 15)
	stmt := fmt.Sprintf(
		`
				INSERT INTO %s
				(account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id,
				test_count, test_selected, source_code_test, new_test, updated_test, repo, source_branch, target_branch)
				VALUES %s`, table, valueStrings)
	stmt = regexp.QuoteMeta(stmt)
	mock.ExpectExec(stmt).
		WithArgs(account, org, project, pipeline, build, stage, step,
			total, selected, src, new, updated, repo, source, target).WillReturnResult(sqlmock.NewResult(0, 1))
	tdb := &TimeScaleDb{Conn: db, SelectionTable: table}
	err = tdb.WriteSelectedTests(ctx, account, org, project, pipeline, build, stage, step, repo, source, target, arg, 0, false)
	assert.Nil(t, err, nil)
}

func Test_WriteSelectedTests_WithUpsert(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "tests"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	stage := "stage"
	step := "step"
	repo := "repo"
	source := "source"
	target := "target"

	total := 20
	selected := 10
	new := 5
	updated := 3
	src := 2

	arg := types.SelectTestsResp{
		TotalTests:    total,
		SelectedTests: selected,
		NewTests:      new,
		UpdatedTests:  updated,
		SrcCodeTests:  src,
		SelectAll:     false,
		Tests:         nil,
	}

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}

	overviewQuery := fmt.Sprintf(
		`
		SELECT test_count, source_code_test, new_test, updated_test, time_taken_ms, time_saved_ms, repo, source_branch, target_branch
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND step_id = $6 AND stage_id = $7`, table)
	overviewQuery = regexp.QuoteMeta(overviewQuery)
	col := []string{"test_count", "source_code_test", "new_test", "updated_test", "time_taken_ms", "time_saved_ms", "repo", "source_branch", "target_branch"}
	rows := sqlmock.NewRows(col).
		AddRow(300, 10, 5, 5, 40, 0, "repo", "source", "target")
	mock.ExpectQuery(overviewQuery).WithArgs(account, org, project, pipeline, build, step, stage).WillReturnRows(rows)

	avgQuery := fmt.Sprintf(
		`
				SELECT AVG(time_taken_ms) FROM (SELECT time_taken_ms FROM %s
				WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND stage_id = $5 AND step_id = $6 AND time_taken_ms != 0 AND test_selected != 0 AND test_count = test_selected LIMIT 10000)
				AS avg`, table)
	avgQuery = regexp.QuoteMeta(avgQuery)
	rows = sqlmock.NewRows([]string{"avg"}).AddRow(150)
	mock.ExpectQuery(avgQuery).WithArgs(account, org, project, pipeline, stage, step).WillReturnRows(rows)

	// Calculation of time saved:
	// Get average = 150 ms
	// Time taken = 30 ms
	// Time saved = 120 ms

	stmt := fmt.Sprintf(
		`
				UPDATE %s
				SET test_count = test_count + $1, test_selected = test_selected + $2,
				source_code_test = source_code_test + $3, new_test = new_test + $4, updated_test = updated_test + $5, time_taken_ms = $6, time_saved_ms = $7
				WHERE account_id = $8 AND org_id = $9 AND project_id = $10 AND pipeline_id = $11 AND build_id = $12 AND step_id = $13 AND stage_id = $14
				`, table)
	stmt = regexp.QuoteMeta(stmt)
	mock.ExpectExec(stmt).
		WithArgs(total, selected, src, new, updated, 30, 120,
			account, org, project, pipeline, build, step, stage).WillReturnResult(sqlmock.NewResult(0, 1))
	tdb := &TimeScaleDb{Conn: db, SelectionTable: table}
	err = tdb.WriteSelectedTests(ctx, account, org, project, pipeline, build, stage, step, repo, source, target, arg, 30, true)
	assert.Nil(t, err, nil)
}

func Test_GetDiffFiles(t *testing.T) {
	ctrl, ctx := gomock.WithContext(context.Background(), t)
	defer ctrl.Finish()

	table := "coverage"
	account := "account"
	org := "org"
	project := "project"
	pipeline := "pipeline"
	build := "build"
	stage := "stage"
	step := "step"
	sha := "sha"
	path1 := "path/to/1.java"
	path2 := "path/to/2.java"
	path3 := "path/to/3.java"

	log := zap.NewExample().Sugar()
	db, mock, err := db.NewMockDB(log)
	if err != nil {
		t.Fatal(err)
	}

	query := fmt.Sprintf(
		`
		SELECT sha, file_path, status
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND step_id = $6 AND stage_id = $7`, table)
	query = regexp.QuoteMeta(query)
	col := []string{"sha", "file_path", "status"}
	rows := sqlmock.NewRows(col).
		AddRow(sha, path1, types.FileModified).
		AddRow(sha, path2, types.FileAdded).
		AddRow(sha, path3, types.FileDeleted)
	mock.ExpectQuery(query).WithArgs(account, org, project, pipeline, build, stage, step).WillReturnRows(rows)

	tdb := &TimeScaleDb{Conn: db, CoverageTable: table}
	resp, err := tdb.GetDiffFiles(ctx, account, org, project, pipeline, build, step, stage)
	assert.Nil(t, err, nil)
	assert.Equal(t, resp.Sha, sha)
	assert.Equal(t, len(resp.Files), 3)
	assert.Contains(t, resp.Files, types.File{Name: path1, Status: types.FileModified})
	assert.Contains(t, resp.Files, types.File{Name: path2, Status: types.FileAdded})
	assert.Contains(t, resp.Files, types.File{Name: path3, Status: types.FileDeleted})
}
