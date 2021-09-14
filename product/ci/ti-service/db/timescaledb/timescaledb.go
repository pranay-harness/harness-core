// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package timescaledb

import (
	"context"
	"errors"
	"fmt"
	"github.com/wings-software/portal/commons/go/lib/db"
	"go.uber.org/zap"
	"gopkg.in/guregu/null.v4/zero"
	"strconv"
	"strings"
	"time"

	"github.com/wings-software/portal/product/ci/ti-service/logger"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

const (
	defaultOffset = "0"
	defaultLimit  = "100"
	asc           = "ASC"
	desc          = "DESC"
)

var (
	writeBatchSize = 1000 // Write this many test cases at a time
	now            = time.Now
)

// TimeScaleDb is a wrapper on top of a timescale DB connection.
type TimeScaleDb struct {
	Conn           *db.DB
	EvalTable      string // table for test reports
	CoverageTable  string // table for file coverage
	SelectionTable string // table for test selection stats for test intelligence
}

// New connects to timescaledb and returns a wrapped connection object.
func New(username, password, host, port, dbName,
	evalTable, coverageTable, selectionTable string,
	enableSSL bool, sslCertPath string, log *zap.SugaredLogger) (*TimeScaleDb, error) {
	iport, err := strconv.ParseUint(port, 10, 64)
	if err != nil {
		return nil, err
	}

	ci := &db.ConnectionInfo{Application: "ti-svc", DBName: dbName,
		User: username, Host: host, Password: password,
		Port: uint(iport), Engine: "postgres",
		EnableSSL: enableSSL, SSLCertPath: sslCertPath}
	db, err := db.NewDB(ci, log)
	if err != nil {
		return nil, err
	}

	// Send a ping to timescale. Set timeout to 10 seconds
	ctx, _ := context.WithTimeout(context.Background(), 10*time.Second)
	err = db.PingContext(ctx)
	if err != nil {
		return nil, err
	}
	return &TimeScaleDb{Conn: db, EvalTable: evalTable, CoverageTable: coverageTable, SelectionTable: selectionTable}, nil
}

func constructPsqlInsertStmt(low, high int) string {
	s := "("
	for i := low; i <= high; i++ {
		s = s + fmt.Sprintf("$%d", i)
		if i != high {
			s = s + ", "
		}
	}
	s = s + ")"
	return s
}

func (tdb *TimeScaleDb) Ping(ctx context.Context) error {
	err := tdb.Conn.PingContext(ctx)
	if err != nil {
		return err
	}
	return nil
}

// Write writes test cases to DB
func (tdb *TimeScaleDb) Write(ctx context.Context, accountId, orgId, projectId, pipelineId,
	buildId, stageId, stepId, report, repo, sha string, tests ...*types.TestCase) error {
	t := now()
	entries := 21
	valueStrings := make([]string, 0, len(tests))
	valueArgs := make([]interface{}, 0, len(tests)*entries)
	i := 1
	cnt := 0
	for _, test := range tests {
		// Do a batch insert to avoid frequent DB calls
		valueStrings = append(valueStrings, constructPsqlInsertStmt(i, i+entries-1))
		valueArgs = append(valueArgs, t, accountId, orgId, projectId, pipelineId, buildId, stageId, stepId, report, repo, sha, test.Name, test.SuiteName,
			test.ClassName, test.DurationMs, test.Result.Status, test.Result.Message, test.Result.Type, test.Result.Desc,
			test.SystemOut, test.SystemErr)
		i = i + entries
		cnt++
		if cnt%writeBatchSize == 0 {
			stmt := fmt.Sprintf(
				`
					INSERT INTO %s
					(created_at, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, report, repo, commit_id, name, suite_name,
					class_name, duration_ms, result, message, type, description, stdout, stderr)
					VALUES %s`, tdb.EvalTable, strings.Join(valueStrings, ","))
			_, err := tdb.Conn.Exec(stmt, valueArgs...)
			if err != nil {
				logger.FromContext(ctx).Errorw("could not write test data to database", zap.Error(err))
				return err
			}
			// Reset all the values
			cnt = 0
			i = 1
			valueStrings = []string{}
			valueArgs = []interface{}{}
		}
	}
	if cnt > 0 {
		stmt := fmt.Sprintf(
			`
				INSERT INTO %s
				(created_at, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, report, repo, commit_id, name, suite_name,
				class_name, duration_ms, result, message, type, description, stdout, stderr)
				VALUES %s`, tdb.EvalTable, strings.Join(valueStrings, ","))
		_, err := tdb.Conn.Exec(stmt, valueArgs...)
		if err != nil {
			logger.FromContext(ctx).Errorw("could not write test data to database", zap.Error(err))
			return err
		}
	}
	return nil
}

// Summary provides test case summary by querying the DB
func (tdb *TimeScaleDb) Summary(ctx context.Context, accountId, orgId, projectId, pipelineId,
	buildId, stepId, stageId, report string) (types.SummaryResponse, error) {
	query := fmt.Sprintf(`
		SELECT duration_ms, result, name FROM %s WHERE account_id = $1
		AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND step_id = $6 AND stage_id = $7 AND report = $8;`, tdb.EvalTable)

	rows, err := tdb.Conn.QueryContext(ctx, query, accountId, orgId, projectId, pipelineId, buildId, stepId, stageId, report)
	if err != nil {
		logger.FromContext(ctx).Errorw("could not query database for test summary", zap.Error(err))
		return types.SummaryResponse{}, err
	}
	total := 0
	var timeTakenMs int64
	tests := []types.TestSummary{}
	for rows.Next() {
		var zdur zero.Int
		var status string
		var testName string
		err = rows.Scan(&zdur, &status, &testName)
		if err != nil {
			// Log error and return
			logger.FromContext(ctx).Errorw("could not read summary response from DB", zap.Error(err))
			return types.SummaryResponse{}, err
		}
		total++
		timeTakenMs = timeTakenMs + zdur.ValueOrZero()
		tests = append(tests, types.TestSummary{Name: testName, Status: types.Status(status)})
	}
	if rows.Err() != nil {
		return types.SummaryResponse{}, rows.Err()
	}
	return types.SummaryResponse{Tests: tests, TotalTests: total, TimeMs: timeTakenMs}, nil
}

func (tdb *TimeScaleDb) GetReportsInfo(ctx context.Context, accountId, orgId, projectId, pipelineId,
	buildId string) ([]types.StepInfo, error) {
	query := fmt.Sprintf(`
		SELECT DISTINCT step_id, stage_id FROM %s WHERE account_id = $1
		AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5`, tdb.EvalTable)

	res := []types.StepInfo{}
	m := make(map[types.StepInfo]struct{})

	rows, err := tdb.Conn.QueryContext(ctx, query, accountId, orgId, projectId, pipelineId, buildId)
	defer rows.Close()
	if err != nil {
		logger.FromContext(ctx).Errorw("could not query database for test summary", zap.Error(err))
		return res, err
	}

	for rows.Next() {
		var stepId zero.String
		var stageId zero.String
		err = rows.Scan(&stepId, &stageId)
		if err != nil {
			// Log error and return
			logger.FromContext(ctx).Errorw("could not read step/stage response from DB", zap.Error(err))
			return res, err
		}
		info := types.StepInfo{Stage: stageId.ValueOrZero(), Step: stepId.ValueOrZero()}
		if _, ok := m[info]; !ok {
			res = append(res, info)
			m[info] = struct{}{}
		}
	}
	if rows.Err() != nil {
		return res, rows.Err()
	}
	return res, nil
}

func (tdb *TimeScaleDb) GetIntelligenceInfo(ctx context.Context, accountId, orgId, projectId, pipelineId,
	buildId string) ([]types.StepInfo, error) {
	query := fmt.Sprintf(`
		SELECT DISTINCT step_id, stage_id FROM %s WHERE account_id = $1
		AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5`, tdb.SelectionTable)

	res := []types.StepInfo{}
	m := make(map[types.StepInfo]struct{})

	rows, err := tdb.Conn.QueryContext(ctx, query, accountId, orgId, projectId, pipelineId, buildId)
	defer rows.Close()
	if err != nil {
		logger.FromContext(ctx).Errorw("could not query database for test summary", zap.Error(err))
		return res, err
	}

	for rows.Next() {
		var stepId zero.String
		var stageId zero.String
		err = rows.Scan(&stepId, &stageId)
		if err != nil {
			// Log error and return
			logger.FromContext(ctx).Errorw("could not read step/stage response from DB", zap.Error(err))
			return res, err
		}
		info := types.StepInfo{Stage: stageId.ValueOrZero(), Step: stepId.ValueOrZero()}
		if _, ok := m[info]; !ok {
			res = append(res, info)
			m[info] = struct{}{}
		}
	}
	if rows.Err() != nil {
		return res, rows.Err()
	}
	return res, nil
}

// GetTestCases returns test cases after querying the DB
func (tdb *TimeScaleDb) GetTestCases(
	ctx context.Context, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId,
	report, suiteName, sortAttribute, status, order, limit, offset string) (types.TestCases, error) {
	statusFilter := "'failed', 'error', 'passed', 'skipped'"
	defaultSortAttribute := "name"
	defaultOrder := asc
	if status == "failed" {
		statusFilter = "'failed', 'error'"
	} else if status != "" {
		return types.TestCases{}, errors.New("status filter only supports 'failed'")
	}
	// default order is to display failed and errored tests first
	failureOrder := `
	CASE
		WHEN result = 'failed' THEN 1
		WHEN result = 'error' THEN 2
		WHEN result = 'skipped' THEN 3
		WHEN result = 'passed' THEN 4
		ELSE 5
	END`
	if offset == "" {
		offset = defaultOffset
	}
	if limit == "" {
		limit = defaultLimit
	}
	if order == "" {
		order = asc
	} else if order != asc && order != desc {
		return types.TestCases{}, errors.New("order must be one of: [ASC, DESC]")
	}
	sortAllowed := []string{"name", "class_name", "status", "duration_ms", ""} // allowed values to sort on
	var ok bool
	for _, s := range sortAllowed {
		if sortAttribute == s {
			ok = true
		}
	}
	if !ok {
		return types.TestCases{}, fmt.Errorf("sorting allowed only for fields: %s", sortAllowed)
	}
	if sortAttribute == "" || sortAttribute == "status" {
		sortAttribute = failureOrder // In case no sort order is specified or we want to sort by status
	}
	query := fmt.Sprintf(
		`
		SELECT name, suite_name, class_name, duration_ms, result, message,
		description, type, stdout, stderr, COUNT(*) OVER() AS full_count
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND step_id = $6 AND stage_id = $7 AND report = $8 AND suite_name = $9 AND result IN (%s)
		ORDER BY %s %s, %s %s
		LIMIT $10 OFFSET $11;`, tdb.EvalTable, statusFilter, sortAttribute, order, defaultSortAttribute, defaultOrder)

	rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId, report, suiteName, limit, offset)
	if err != nil {
		logger.FromContext(ctx).Errorw("could not query database for test cases", zap.Error(err))
		return types.TestCases{}, err
	}
	tests := []types.TestCase{}
	total := 0
	for rows.Next() {
		var t types.TestCase
		// Postgres may return null for empty strings for some versions
		var zsuite, zclass, zmessage, zdesc, ztype, zout, zerr zero.String
		var zdur zero.Int
		err = rows.Scan(&t.Name, &zsuite, &zclass, &zdur, &t.Result.Status, &zmessage, &zdesc, &ztype, &zout, &zerr, &total)
		if err != nil {
			// Log error and return
			logger.FromContext(ctx).Errorw("could not read test case response from DB", zap.Error(err))
			return types.TestCases{}, err
		}
		t.SuiteName = zsuite.ValueOrZero()
		t.ClassName = zclass.ValueOrZero()
		t.Result.Message = zmessage.ValueOrZero()
		t.Result.Desc = zdesc.ValueOrZero()
		t.Result.Type = ztype.ValueOrZero()
		t.SystemOut = zout.ValueOrZero()
		t.SystemErr = zerr.ValueOrZero()
		t.DurationMs = zdur.ValueOrZero()
		tests = append(tests, t)
	}
	if rows.Err() != nil {
		return types.TestCases{}, rows.Err()
	}
	pageSize, err := strconv.Atoi(limit)
	if err != nil {
		return types.TestCases{}, err
	}
	numPages := total / pageSize
	if total%pageSize != 0 {
		numPages++
	}

	metadata := types.ResponseMetadata{TotalItems: total, PageSize: pageSize, PageItemCount: len(tests), TotalPages: numPages}
	return types.TestCases{Metadata: metadata, Tests: tests}, nil
}

// GetTestSuites returns test suites after querying the DB
func (tdb *TimeScaleDb) GetTestSuites(
	ctx context.Context, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId,
	report, sortAttribute, status, order, limit, offset string) (types.TestSuites, error) {
	defaultSortAttribute := "suite_name"
	defaultOrder := asc
	statusFilter := "'failed', 'error', 'passed', 'skipped'"
	if status == "failed" {
		statusFilter = "'failed', 'error'"
	} else if status != "" {
		return types.TestSuites{}, errors.New("status filter only supports 'failed'")
	}
	if offset == "" {
		offset = defaultOffset
	}
	if limit == "" {
		limit = defaultLimit
	}
	sortAllowed := []string{"suite_name", "duration_ms", "total_tests", "skipped_tests", "passed_tests", "failed_tests", "fail_pct", ""} // allowed values to sort on
	var ok bool
	for _, s := range sortAllowed {
		if sortAttribute == s {
			ok = true
		}
	}
	if !ok {
		return types.TestSuites{}, fmt.Errorf("sorting allowed only for fields: %s", sortAllowed)
	}
	// If sort attribute is not set, set it to failure rate
	if sortAttribute == "" {
		sortAttribute = "fail_pct"
		order = desc
	}
	// If order is not set, use ascending order
	if order == "" {
		order = asc
	} else if order != asc && order != desc {
		return types.TestSuites{}, errors.New("order must be one of: [ASC, DESC]")
	}
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
		LIMIT $9 OFFSET $10;`, tdb.EvalTable, statusFilter, sortAttribute, order, defaultSortAttribute, defaultOrder)
	rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId, report, limit, offset)
	if err != nil {
		logger.FromContext(ctx).Errorw("could not query database for test suites", "error_msg", err)
		return types.TestSuites{}, err
	}
	testSuites := []types.TestSuite{}
	total := 0
	for rows.Next() {
		var t types.TestSuite
		var zdur zero.Int
		err = rows.Scan(&t.Name, &zdur, &t.TotalTests, &t.SkippedTests, &t.PassedTests, &t.FailedTests, &t.FailPct, &total)
		if err != nil {
			// Log the error and return
			logger.FromContext(ctx).Errorw("could not read suite response from DB", zap.Error(err))
			return types.TestSuites{}, err
		}
		t.DurationMs = zdur.ValueOrZero()
		testSuites = append(testSuites, t)
	}
	if rows.Err() != nil {
		return types.TestSuites{}, rows.Err()
	}
	pageSize, err := strconv.Atoi(limit)
	if err != nil {
		return types.TestSuites{}, err
	}
	numPages := total / pageSize
	if total%pageSize != 0 {
		numPages++
	}

	metadata := types.ResponseMetadata{TotalItems: total, PageSize: pageSize, PageItemCount: len(testSuites), TotalPages: numPages}
	return types.TestSuites{Metadata: metadata, Suites: testSuites}, nil
}

// WriteSelectedTests write selected test information corresponding to a PR to the database.
// If an entry already exists, it adds the counts to the existing row.
func (tdb *TimeScaleDb) WriteSelectedTests(ctx context.Context, accountID, orgId, projectId, pipelineId,
	buildId, stageId, stepId, repo, source, target string, s types.SelectTestsResp, timeMs int, upsert bool) error {
	entries := 15
	valueArgs := make([]interface{}, 0, entries)
	var stmt string
	if upsert == true {
		// Upsert time_taken_ms and time_saved_ms
		/*
			Calculation of time_saved:
				Get list of 10000 runs for the same step_id over previous builds where num_selected != 0 and time_taken_ms != 0 AND test_count = test_selected
				If there are none, use average time to run all the tests as 500ms * number of tests
				time_saved = (Total tests skipped) * (Average time per test)
				if time_saved < 0
					time_saved = 0
		*/
		overview, err := tdb.GetSelectionOverview(ctx, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId)
		if err != nil {
			return err
		}
		query := fmt.Sprintf(
			`
				SELECT AVG(time_taken_ms) FROM (SELECT time_taken_ms FROM %s
				WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND stage_id = $5 AND step_id = $6 AND time_taken_ms != 0 AND test_selected != 0 AND test_count = test_selected LIMIT 10000)
				AS avg`, tdb.SelectionTable)
		rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, pipelineId, stageId, stepId)
		if err != nil {
			return rows.Err()
		}
		defer rows.Close()
		avgTotalTime := 500 * overview.Total
		for rows.Next() {
			var zdur zero.Float
			err = rows.Scan(&zdur)
			if err != nil {
				logger.FromContext(ctx).Errorw("could not get average test time", zap.Error(err))
				break
			}
			if zdur.ValueOrZero() != 0 {
				avgTotalTime = int(zdur.ValueOrZero())
			}
			break
		}
		timeSavedMs := avgTotalTime - timeMs
		// If no tests were skipped or the job took more time than the average time for a full run,
		// set the time saved as 0
		if timeSavedMs < 0 || overview.Skipped == 0 {
			timeSavedMs = 0
		}

		stmt = fmt.Sprintf(
			`
					UPDATE %s
					SET test_count = test_count + $1, test_selected = test_selected + $2,
					source_code_test = source_code_test + $3, new_test = new_test + $4, updated_test = updated_test + $5, time_taken_ms = $6, time_saved_ms = $7
					WHERE account_id = $8 AND org_id = $9 AND project_id = $10 AND pipeline_id = $11 AND build_id = $12 AND step_id = $13 AND stage_id = $14
					`, tdb.SelectionTable)
		valueArgs = append(valueArgs, s.TotalTests, s.SelectedTests, s.SrcCodeTests, s.NewTests, s.UpdatedTests, timeMs, timeSavedMs,
			accountID, orgId, projectId, pipelineId, buildId, stepId, stageId)
	} else {
		stmt = fmt.Sprintf(
			`
					INSERT INTO %s
					(account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id,
					test_count, test_selected, source_code_test, new_test, updated_test, repo, source_branch, target_branch)
					VALUES %s`, tdb.SelectionTable, constructPsqlInsertStmt(1, entries))
		valueArgs = append(valueArgs, accountID, orgId, projectId, pipelineId, buildId, stageId, stepId,
			s.TotalTests, s.SelectedTests, s.SrcCodeTests, s.NewTests, s.UpdatedTests, repo, source, target)
	}

	_, err := tdb.Conn.ExecContext(ctx, stmt, valueArgs...)
	if err != nil {
		logger.FromContext(ctx).Errorw("could not write test data to database", zap.Error(err))
		return err
	}
	return nil
}

// GetSelectionOverview provides high level stats for test selection
func (tdb *TimeScaleDb) GetSelectionOverview(ctx context.Context, accountID, orgId, projectId, pipelineId,
	buildId, stepId, stageId string) (types.SelectionOverview, error) {
	var ztotal, zsrc, znew, zupd, ztt, zts zero.Int
	var repo, source, target zero.String
	res := types.SelectionOverview{}
	query := fmt.Sprintf(
		`
		SELECT test_count, source_code_test, new_test, updated_test, time_taken_ms, time_saved_ms, repo, source_branch, target_branch
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND step_id = $6 AND stage_id = $7`, tdb.SelectionTable)
	rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId)
	defer rows.Close()
	if err != nil {
		logger.FromContext(ctx).Errorw("could not query database for selection overview", zap.Error(err))
		return res, err
	}
	for rows.Next() {
		err = rows.Scan(&ztotal, &zsrc, &znew, &zupd, &ztt, &zts, &repo, &source, &target)
		if err != nil {
			logger.FromContext(ctx).Errorw("could not read overview response from db", zap.Error(err))
			return res, err
		}
		res.Total = int(ztotal.ValueOrZero())
		res.TimeSavedMs = int(zts.ValueOrZero())
		res.TimeTakenMs = int(ztt.ValueOrZero())
		res.Repo = repo.ValueOrZero()
		res.SourceBranch = source.ValueOrZero()
		res.TargetBranch = target.ValueOrZero()
		res.Selected.New = int(znew.ValueOrZero())
		res.Selected.Upd = int(zupd.ValueOrZero())
		res.Selected.Src = int(zsrc.ValueOrZero())
		res.Skipped = res.Total - res.Selected.New - res.Selected.Upd - res.Selected.Src
		break
	}
	if rows.Err() != nil {
		return res, rows.Err()
	}

	return res, nil
}

// WriteDiffFiles writes the changed files in a PR along with their status (modified/added/deleted)
func (tdb *TimeScaleDb) WriteDiffFiles(ctx context.Context, accountID, orgId, projectId, pipelineId,
	buildId, stageId, stepId string, diff types.DiffInfo) error {
	entries := 10
	i := 1
	valueStrings := make([]string, 0, len(diff.Files))
	valueArgs := make([]interface{}, 0, len(diff.Files)*entries)
	for _, f := range diff.Files {
		valueStrings = append(valueStrings, constructPsqlInsertStmt(i, i+entries-1))
		valueArgs = append(valueArgs, accountID, orgId, projectId, pipelineId, buildId, stageId, stepId, diff.Sha, f.Name, f.Status)
		i = i + entries
	}
	stmt := fmt.Sprintf(
		`
					INSERT INTO %s
					(account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, sha, file_path, status)
					VALUES %s`, tdb.CoverageTable, strings.Join(valueStrings, ","))
	_, err := tdb.Conn.ExecContext(ctx, stmt, valueArgs...)
	if err != nil {
		logger.FromContext(ctx).Errorw("could not write file information to database", zap.Error(err))
		return err
	}
	return nil
}

// GetDiffFiles returns the modified files in a PR corresponding to a list of commits in a
// pull request.
func (tdb *TimeScaleDb) GetDiffFiles(ctx context.Context, accountID, orgId, projectId, pipelineId,
	buildId, stageId, stepId string) (types.DiffInfo, error) {
	res := types.DiffInfo{}
	var sha, path, status zero.String
	query := fmt.Sprintf(
		`
		SELECT sha, file_path, status
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND step_id = $6 AND stage_id = $7`, tdb.CoverageTable)

	rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, pipelineId, buildId, stepId, stageId)
	defer rows.Close()

	if err != nil {
		logger.FromContext(ctx).Errorw("could not query database for changed files", zap.Error(err))
		return res, err
	}
	for rows.Next() {
		err = rows.Scan(&sha, &path, &status)
		if err != nil {
			logger.FromContext(ctx).Errorw("could not read overview response from db", zap.Error(err))
			return res, err
		}
		if path.ValueOrZero() != "" {
			res.Sha = sha.ValueOrZero()
			res.Files = append(res.Files, types.File{Name: path.ValueOrZero(), Status: types.ConvertToFileStatus(status.ValueOrZero())})
		}
	}
	if rows.Err() != nil {
		return res, rows.Err()
	}

	return res, nil
}
