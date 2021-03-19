package handler

import (
	"encoding/json"
	"fmt"
	"github.com/wings-software/portal/product/ci/addon/ti"
	"github.com/wings-software/portal/product/ci/common/avro"
	"net/http"
	"time"

	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/tidb"
	"go.uber.org/zap"
)

const (
	// path of this needs to be decided [TODO: Aman]
	cgSchemaPath = "/Users/amansingh/code/portal/product/ci/common/avro/schema.avsc" 
)

// HandleSelect returns an http.HandlerFunc that figures out which tests to run
// based on the files provided.
func HandleSelect(tidb tidb.TiDB, db db.Db, config config.Config, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()

		// TODO: Use this information while retrieving from TIDB
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, pipelineIdParam, buildIdParam,
			stageIdParam, stepIdParam, repoParam, shaParam, branchParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}
		accountId := r.FormValue(accountIDParam)
		orgId := r.FormValue(orgIdParam)
		projectId := r.FormValue(projectIdParam)
		pipelineId := r.FormValue(pipelineIdParam)
		buildId := r.FormValue(buildIdParam)
		stageId := r.FormValue(stageIdParam)
		stepId := r.FormValue(stepIdParam)
		repo := r.FormValue(repoParam)
		branch := r.FormValue(branchParam)
		sha := r.FormValue(shaParam)

		var files []string
		if err := json.NewDecoder(r.Body).Decode(&files); err != nil {
			WriteBadRequest(w, err)
			log.Errorw("api: could not unmarshal input for test selection",
				"account_id", accountId, "repo", repo, "branch", branch, "sha", sha, zap.Error(err))
			return
		}
		log.Infow("got a files list", "account_id", accountId, "files", files, "repo", repo, "branch", branch, "sha", sha)

		// Make call to Mongo DB to get the tests to run
		selected, err := tidb.GetTestsToRun(ctx, files)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not select tests", "account_id", accountId,
				"repo", repo, "branch", branch, "sha", sha, zap.Error(err))
			return
		}

		// Classify and write the test selection stats to timescaleDB
		err = db.WriteSelectedTests(ctx, config.TimeScaleDb.SelectionTable, accountId, orgId,
			projectId, pipelineId, buildId, stageId, stepId, selected)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not write selected tests", "account_id", accountId,
				"repo", repo, "branch", branch, "sha", sha, zap.Error(err))
			return
		}

		// Write the selected tests back
		WriteJSON(w, selected.Tests, 200)
		log.Infow("completed test selection", "account_id", accountId,
			"repo", repo, "branch", branch, "sha", sha, "tests", selected.Tests,
			"num_tests", len(selected.Tests), "time_taken", time.Since(st))

	}
}

func HandleOverview(db db.Db, config config.Config, log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()

		// TODO: Use this information while retrieving from TIDB
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, pipelineIdParam, buildIdParam)
		if err != nil {
			WriteInternalError(w, err)
			return
		}
		accountId := r.FormValue(accountIDParam)
		orgId := r.FormValue(orgIdParam)
		projectId := r.FormValue(projectIdParam)
		pipelineId := r.FormValue(pipelineIdParam)
		buildId := r.FormValue(buildIdParam)

		overview, err := db.GetSelectionOverview(ctx, config.TimeScaleDb.SelectionTable, accountId,
			orgId, projectId, pipelineId, buildId)
		if err != nil {
			log.Errorw("could not get TI overview from DB", zap.Error(err))
			WriteInternalError(w, err)
			return
		}

		// Write the selected tests back
		WriteJSON(w, overview, 200)
		log.Infow("retrieved test overview", "account_id", accountId, "time_taken", time.Since(st))

	}
}


func UploadCg( log *zap.SugaredLogger) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var data []byte
		if err := json.NewDecoder(r.Body).Decode(&data); err != nil {
			WriteBadRequest(w, err)
			log.Errorw("could not unmarshal request body")
		}
		cgSer, err := avro.NewCgphSerialzer(cgSchemaPath)
		if err != nil {
			log.Errorf("failed to create callgraph serializer instance", zap.Error(err))
			WriteInternalError(w, err)
			return
		}
		cgString, err := cgSer.Deserialize(data)
		if err != nil {
			log.Errorf("failed to deserialize callgraph", zap.Error(err))
			WriteInternalError(w, err)
			return
		}

		cg, err := ti.FromStringMap(cgString.(map[string]interface{}))
		if err != nil {
			log.Errorf("failed to construct callgraph object from interface object", zap.Error(err))
			WriteInternalError(w, err)
			return
		}
		writeCg()
	}
}

func writeCg () {
	return
}