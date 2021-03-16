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

	"github.com/wings-software/portal/product/ci/ti-service/types"
)

const (
	defaultOffset  = "0"
	defaultLimit   = "100"
	asc            = "ASC"
	desc           = "DESC"
	writeBatchSize = 1000 // Write this many test cases at a time
)

var (
	now = time.Now
)

// TimeScaleDb is a wrapper on top of a timescale DB connection.
type TimeScaleDb struct {
	Conn *db.DB
	Log  *zap.SugaredLogger
}

// New connects to timescaledb and returns a wrapped connection object.
func New(username, password, host, port, dbName string, log *zap.SugaredLogger) (*TimeScaleDb, error) {
	iport, err := strconv.ParseUint(port, 10, 64)
	if err != nil {
		return nil, err
	}

	ci := &db.ConnectionInfo{Application: "ti-svc", DBName: dbName, User: username, Host: host, Password: password, Port: uint(iport), Engine: "postgres"}
	db, err := db.NewDB(ci, log)
	if err != nil {
		return nil, err
	}

	return &TimeScaleDb{Conn: db, Log: log}, nil
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

// Write writes test cases to DB
func (tdb *TimeScaleDb) Write(ctx context.Context, table, accountId, orgId, projectId, pipelineId,
	buildId, stageId, stepId, report string, tests ...*types.TestCase) error {
	t := now()
	entries := 19
	valueStrings := make([]string, 0, len(tests))
	valueArgs := make([]interface{}, 0, len(tests)*entries)
	i := 1
	cnt := 0
	for _, test := range tests {
		// Do a batch insert to avoid frequent DB calls
		valueStrings = append(valueStrings, constructPsqlInsertStmt(i, i+entries-1))
		valueArgs = append(valueArgs, t, accountId, orgId, projectId, pipelineId, buildId, stageId, stepId, report, test.Name, test.SuiteName,
			test.ClassName, test.DurationMs, test.Result.Status, test.Result.Message, test.Result.Type, test.Result.Desc,
			test.SystemOut, test.SystemErr)
		i = i + entries
		cnt++
		if cnt%writeBatchSize == 0 {
			stmt := fmt.Sprintf(
				`
					INSERT INTO %s
					(created_at, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, report, name, suite_name,
					class_name, duration_ms, result, message, type, description, stdout, stderr)
					VALUES %s`, table, strings.Join(valueStrings, ","))
			_, err := tdb.Conn.Exec(stmt, valueArgs...)
			if err != nil {
				tdb.Log.Errorw("could not write test data to database", zap.Error(err))
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
				(created_at, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id, report, name, suite_name,
				class_name, duration_ms, result, message, type, description, stdout, stderr)
				VALUES %s`, table, strings.Join(valueStrings, ","))
		_, err := tdb.Conn.Exec(stmt, valueArgs...)
		if err != nil {
			tdb.Log.Errorw("could not write test data to database", zap.Error(err))
			return err
		}
	}
	return nil
}

// Summary provides test case summary by querying the DB
func (tdb *TimeScaleDb) Summary(ctx context.Context, table, accountId, orgId, projectId, pipelineId,
	buildId, report string) (types.SummaryResponse, error) {
	query := fmt.Sprintf(`
		SELECT duration_ms, result, name FROM %s WHERE account_id = $1
		AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND report = $6;`, table)

	rows, err := tdb.Conn.QueryContext(ctx, query, accountId, orgId, projectId, pipelineId, buildId, report)
	if err != nil {
		tdb.Log.Errorw("could not query database for test summary", zap.Error(err))
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
			tdb.Log.Errorw("could not read summary response from DB", zap.Error(err))
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

// GetTestCases returns test cases after querying the DB
func (tdb *TimeScaleDb) GetTestCases(
	ctx context.Context, table, accountID, orgId, projectId, pipelineId, buildId,
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
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND report = $6 AND suite_name = $7 AND result IN (%s)
		ORDER BY %s %s, %s %s
		LIMIT $8 OFFSET $9;`, table, statusFilter, sortAttribute, order, defaultSortAttribute, defaultOrder)
	rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, pipelineId, buildId, report, suiteName, limit, offset)
	if err != nil {
		tdb.Log.Errorw("could not query database for test cases", zap.Error(err))
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
			tdb.Log.Errorw("could not read test case response from DB", zap.Error(err))
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
	ctx context.Context, table, accountID, orgId, projectId, pipelineId, buildId,
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
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5 AND report = $6 AND result IN (%s)
		GROUP BY suite_name
		ORDER BY %s %s, %s %s
		LIMIT $7 OFFSET $8;`, table, statusFilter, sortAttribute, order, defaultSortAttribute, defaultOrder)
	rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, pipelineId, buildId, report, limit, offset)
	if err != nil {
		tdb.Log.Errorw("could not query database for test suites", "error_msg", err)
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
			tdb.Log.Errorw("could not read suite response from DB", zap.Error(err))
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

func (tdb *TimeScaleDb) WriteSelectedTests(ctx context.Context, table, accountID, orgId, projectId, pipelineId,
	buildId, stageId, stepId string, selected types.SelectTestsResponse) error {
	sel := 0
	src := 0
	new := 0
	upd := 0
	t := now()
	for _, t := range selected.Tests {
		if t.Selection == types.SelectNewTest {
			new += 1
		} else if t.Selection == types.SelectUpdatedTest {
			upd += 1
		} else {
			src += 1
		}
		sel += 1
	}
	entries := 13
	valueArgs := make([]interface{}, 0, entries)
	valueArgs = append(valueArgs, t, accountID, orgId, projectId, pipelineId, buildId, stageId, stepId,
		selected.TotalTests, sel, src, new, upd)
	stmt := fmt.Sprintf(
		`
					INSERT INTO %s
					(created_at, account_id, org_id, project_id, pipeline_id, build_id, stage_id, step_id,
					test_count, test_selected, source_code_test, new_test, updated_test)
					VALUES %s`, table, constructPsqlInsertStmt(1, entries))
	_, err := tdb.Conn.Exec(stmt, valueArgs...)
	if err != nil {
		tdb.Log.Errorw("could not write test data to database", zap.Error(err))
		return err
	}
	return nil
}

func (tdb *TimeScaleDb) GetSelectionOverview(ctx context.Context, table, accountID, orgId, projectId, pipelineId,
	buildId string) (types.SelectionOverview, error) {
	var ztotal, zsrc, znew, zupd zero.Int
	query := fmt.Sprintf(
		`
		SELECT test_count, source_code_test, new_test, updated_test
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND pipeline_id = $4 AND build_id = $5`, table)
	rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, pipelineId, buildId)
	if err != nil {
		tdb.Log.Errorw("could not query database for test suites", zap.Error(err))
		return types.SelectionOverview{}, err
	}
	res := types.SelectionOverview{}
	for rows.Next() {
		err = rows.Scan(&ztotal, &zsrc, &znew, &zupd)
		if err != nil {
			tdb.Log.Errorw("could not read overview response from db", zap.Error(err))
			return types.SelectionOverview{}, err
		}
		res.Total = int(ztotal.ValueOrZero())
		res.Selected.New = int(znew.ValueOrZero())
		res.Selected.Upd = int(zupd.ValueOrZero())
		res.Selected.Src = int(zsrc.ValueOrZero())
		res.Skipped = res.Total - res.Selected.New - res.Selected.Upd - res.Selected.Src
		return res, nil
	}
	if rows.Err() != nil {
		return res, rows.Err()
	}

	return res, nil
}
