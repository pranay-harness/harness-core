package handler

import (
	"context"
	"encoding/json"
	"errors"
	"io"
	"net/http"
	"strconv"
	"time"

	cgp "github.com/wings-software/portal/product/ci/addon/parser/cg"
	"github.com/wings-software/portal/product/ci/common/avro"
	"github.com/wings-software/portal/product/ci/ti-service/config"
	"github.com/wings-software/portal/product/ci/ti-service/db"
	"github.com/wings-software/portal/product/ci/ti-service/logger"
	"github.com/wings-software/portal/product/ci/ti-service/tidb"
	"github.com/wings-software/portal/product/ci/ti-service/tidb/mongodb"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.uber.org/zap"
)

const (
	cgSchemaType = "callgraph"
)

// HandleSelect returns an http.HandlerFunc that figures out which tests to run
// based on the files provided.
func HandleSelect(tidb tidb.TiDB, db db.Db, config config.Config) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()
		log := logger.FromContext(ctx)

		// TODO: Use this information while retrieving from TIDB
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, pipelineIdParam, buildIdParam,
			stageIdParam, stepIdParam, repoParam, targetBranchParam)
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
		source := r.FormValue(sourceBranchParam)
		target := r.FormValue(targetBranchParam)
		sha := r.FormValue(shaParam)

		var req types.SelectTestsReq
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			WriteBadRequest(w, err)
			log.Errorw("api: could not unmarshal input for test selection",
				"account_id", accountId, "repo", repo, "source", source, "target", target,
				"sha", sha, zap.Error(err))
			return
		}
		req.TargetBranch = target
		req.Repo = repo
		log.Infow("got a files list", "account_id", accountId, "files", req.Files,
			"repo", repo, "source", source, "target", target, "sha", sha)

		// Make call to Mongo DB to get the tests to run
		selected, err := tidb.GetTestsToRun(ctx, req, accountId, config.MongoDb.EnableReflection)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not select tests", "account_id", accountId,
				"repo", repo, "source", source, "target", target, "sha", sha, zap.Error(err))
			return
		}

		// Classify and write the test selection stats to timescaleDB
		err = db.WriteSelectedTests(ctx, accountId, orgId, projectId, pipelineId, buildId, stageId, stepId, repo, source, target, selected, 0, false)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not write selected tests", "account_id", accountId,
				"repo", repo, "source", source, "target", target, "sha", sha, zap.Error(err))
			return
		}

		// Write changed file information
		err = db.WriteDiffFiles(ctx, accountId, orgId, projectId, pipelineId, buildId,
			stageId, stepId, types.DiffInfo{Sha: sha, Files: req.Files})
		if err != nil {
			log.Errorw("api: could not write changed file information", "account_id", accountId, "build_id", buildId,
				"repo", repo, "source", source, "target", target, "sha", sha, zap.Error(err))
		}

		// Write the selected tests back
		WriteJSON(w, selected, 200)
		log.Infow("completed test selection", "account_id", accountId,
			"repo", repo, "source", source, "target", target, "sha", sha,
			"num_tests", len(selected.Tests), "time_taken", time.Since(st))

	}
}

// HandleVgSearch returns a http.HandlerFunc that returns back the visualization callgraph
// corresponding to a fully qualified class name.
// If a source branch is provided, we first try to retrieve the visualization call graph only from
// the source branch. Otherwise, if the source call graph does not exist, we use the target branch call graph.
// If a class name is not specified, this will provide a partial visualization graph containing at max limit
// number of nodes.
func HandleVgSearch(tidb tidb.TiDB, db db.Db) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()
		log := logger.FromContext(ctx)

		// Info needed:
		// i) account ID, ... buildID
		// ii) fully-qualified class name (optional) (if not specified, we will return a random part of the callgraph)
		// iii) limit (max number of nodes to return in the output) (optional)
		// We use the mapping of <account, org, project, pipeline, build, step, stage> -> <repo, source, target> to
		// get the repo, source and target branch to render visualization data from.
		// If class name is specified, we return the callgraph corresponding starting with that class as a root node.
		// This is done by doing a BFS from that point.

		err := validate(r, accountIDParam, orgIdParam, projectIdParam, pipelineIdParam, buildIdParam, stepIdParam, stageIdParam)
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
		limitStr := r.FormValue(limitParam)
		class := r.FormValue(classNameParam)
		limit, err := strconv.ParseInt(limitStr, 10, 64)
		if err != nil {
			log.Errorw("could not parse limit", zap.Error(err))
			limit = 200 // return back upto 200 nodes if we can't parse the limit
		}

		overview, err := db.GetSelectionOverview(ctx, accountId, orgId, projectId, pipelineId, buildId, stepId, stageId)
		if err != nil {
			WriteInternalError(w, err)
			log.Errorw("api: could not get selection overview for VG search", accountIDParam, accountId, orgIdParam, orgId, projectIdParam, projectId,
				pipelineIdParam, pipelineId, buildIdParam, buildId, stageIdParam, stageId, stepIdParam, stepId, zap.Error(err))
			return
		}

		if overview.Skipped == 0 {
			WriteInternalError(w, errors.New("no call graph is generated when all tests are run"))
			log.Errorw("api: could not get visualisation graph as all tests were selected", accountIDParam, accountId, orgIdParam, orgId, projectIdParam, projectId,
				pipelineIdParam, pipelineId, buildIdParam, buildId, stageIdParam, stageId, stepIdParam, stepId)
			return
		}

		resp, err := db.GetDiffFiles(ctx, accountId, orgId, projectId, pipelineId, buildId, stageId, stepId)
		if err != nil {
			log.Errorw("api: could not get changed files for VG search", accountIDParam, accountId, orgIdParam, orgId, projectIdParam, projectId,
				pipelineIdParam, pipelineId, buildIdParam, buildId, stageIdParam, stageId, stepIdParam, stepId, zap.Error(err))
		}

		req := types.GetVgReq{
			AccountId:    accountId,
			Repo:         overview.Repo,
			SourceBranch: overview.SourceBranch,
			TargetBranch: overview.TargetBranch,
			Class:        class,
			Limit:        limit,
			DiffFiles:    resp.Files,
		}

		log.Infow("api: making call to GetVg", "arg", req, accountIDParam, accountId, orgIdParam, orgId, projectIdParam, projectId,
			pipelineIdParam, pipelineId, buildIdParam, buildId, stageIdParam, stageId, stepIdParam, stepId)

		// Make call to Mongo DB to get the visualization call graph
		// Set timeout of 15 seconds to avoid choking the DB
		ctx, cancel := context.WithTimeout(ctx, 15*time.Second)
		defer cancel()
		graph, err := tidb.GetVg(ctx, req)
		if err != nil {
			WriteInternalError(w, err)
			// TODO: (Vistaar) Find a better way to add all the params to all the different types of logs.
			log.Errorw("api: could not get visualization graph", accountIDParam, accountId, orgIdParam, orgId, projectIdParam, projectId,
				pipelineIdParam, pipelineId, buildIdParam, buildId, stageIdParam, stageId, stepIdParam, stepId,
				repoParam, overview.Repo, sourceBranchParam, overview.SourceBranch, targetBranchParam, overview.TargetBranch,
				classNameParam, class, zap.Error(err))
			return
		}

		// Write the selected tests back
		WriteJSON(w, graph, 200)
		log.Infow("retrieved visualization callgraph", accountIDParam, accountId, orgIdParam, orgId, projectIdParam, projectId,
			pipelineIdParam, pipelineId, buildIdParam, buildId, stageIdParam, stageId, stepIdParam, stepId,
			repoParam, overview.Repo, sourceBranchParam, overview.SourceBranch, targetBranchParam, overview.TargetBranch,
			classNameParam, class, "len(edges)", len(graph.Edges), "len(nodes)", len(graph.Nodes), "time_taken", time.Since(st))

	}
}

func HandleReportsInfo(db db.Db) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()
		log := logger.FromContext(ctx)

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

		resp, err := db.GetReportsInfo(ctx, accountId, orgId, projectId, pipelineId, buildId)
		if err != nil {
			log.Errorw("could not get reports info from DB", zap.Error(err))
			WriteInternalError(w, err)
			return
		}

		// Write the selected tests back
		WriteJSON(w, resp, 200)
		log.Infow("retrieved test report info", "account_id", accountId, "time_taken", time.Since(st))
	}
}

func HandleIntelligenceInfo(db db.Db) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()
		log := logger.FromContext(ctx)

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

		resp, err := db.GetIntelligenceInfo(ctx, accountId, orgId, projectId, pipelineId, buildId)
		if err != nil {
			log.Errorw("could not get test intelligence info from DB", zap.Error(err))
			WriteInternalError(w, err)
			return
		}

		// Write the selected tests back
		WriteJSON(w, resp, 200)
		log.Infow("retrieved test intelligence info", "account_id", accountId, "time_taken", time.Since(st))
	}
}

// HandlePing returns an http.HandlerFunc that pings
// the backends to ensure smooth working of TI service.
func HandlePing(db db.Db) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx, _ := context.WithTimeout(r.Context(), 5*time.Second) // 5 second timeout for pings
		log := logger.FromContext(ctx)

		if err := db.Ping(ctx); err != nil {
			if err != nil {
				log.Errorw("could not ping the data DB", zap.Error(err))
				WriteInternalError(w, err)
				return
			}
		}

		io.WriteString(w, "OK")
	}
}

func HandleOverview(db db.Db) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		st := time.Now()
		ctx := r.Context()
		log := logger.FromContext(ctx)

		// TODO: Use this information while retrieving from TIDB
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, pipelineIdParam, buildIdParam, stepIdParam, stageIdParam)
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

		overview, err := db.GetSelectionOverview(ctx, accountId, orgId, projectId, pipelineId, buildId, stepId, stageId)
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

func HandleUploadCg(tidb tidb.TiDB, db db.Db) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		err := validate(r, accountIDParam, orgIdParam, projectIdParam, repoParam, sourceBranchParam, targetBranchParam)
		if err != nil {
			WriteBadRequest(w, err)
			return
		}
		acc := r.FormValue(accountIDParam)
		org := r.FormValue(orgIdParam)
		proj := r.FormValue(projectIdParam)
		target := r.FormValue(targetBranchParam)
		pipelineId := r.FormValue(pipelineIdParam)
		buildId := r.FormValue(buildIdParam)
		stageId := r.FormValue(stageIdParam)
		stepId := r.FormValue(stepIdParam)
		timeMsStr := r.FormValue(timeMsParam)

		ctx := r.Context()
		log := logger.FromContext(ctx)
		timeMs, err := strconv.ParseInt(timeMsStr, 10, 32)
		if err != nil {
			log.Errorw("could not parse time taken", zap.Error(err))
			timeMs = 0
		}
		info := mongodb.VCSInfo{
			Repo:     r.FormValue(repoParam),
			Branch:   r.FormValue(sourceBranchParam),
			CommitId: r.FormValue(shaParam),
		}
		var data []byte
		if err := json.NewDecoder(r.Body).Decode(&data); err != nil {
			log.Errorw("could not unmarshal request body")
			WriteBadRequest(w, err)
		}

		cgSer, err := avro.NewCgphSerialzer(cgSchemaType)
		if err != nil {
			log.Errorw("failed to create callgraph serializer instance", accountIDParam, acc,
				repoParam, info.Repo, sourceBranchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}

		cgString, err := cgSer.Deserialize(data)
		if err != nil {
			log.Errorw("failed to deserialize callgraph", accountIDParam, acc, repoParam, info.Repo,
				sourceBranchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}

		cg, err := cgp.FromStringMap(cgString.(map[string]interface{}))
		if err != nil {
			log.Errorw("failed to construct callgraph object from interface object",
				accountIDParam, acc, repoParam, info.Repo, sourceBranchParam, info.Branch, zap.Error(err))
			WriteBadRequest(w, err)
			return
		}
		log.Infow("received callgraph", "len(nodes)", len(cg.Nodes), "len(relations)", len(cg.TestRelations),
			"len(vis_relations)", len(cg.VisRelations), accountIDParam, acc, repoParam,
			info.Repo, sourceBranchParam, info.Branch, targetBranchParam, target)

		st := time.Now()
		resp, err := tidb.UploadPartialCg(r.Context(), cg, info, acc, org, proj, target)
		log.Infow("completed partial CG upload to mongo", "account", acc, "org", org, "project", proj, "pipeline", pipelineId, "build", buildId, "stage", stageId, "step", stepId, "time_taken", time.Since(st).String())
		// Try to update counts even if uploading partial CG failed
		werr := db.WriteSelectedTests(r.Context(), acc, org, proj, pipelineId, buildId, stageId, stepId, "", "", "", resp, int(timeMs), true)
		if err != nil {
			log.Errorw("failed to write partial cg to mongo", accountIDParam, acc, orgIdParam, org, projectIdParam, proj, buildIdParam, buildId, repoParam, info.Repo,
				sourceBranchParam, info.Branch, stepIdParam, stepId, stageIdParam, stageId, zap.Error(err))
			WriteBadRequest(w, err)
			return
		} else if werr != nil {
			log.Errorw("failed to update test counts in stats DB", accountIDParam, acc, orgIdParam, org, repoParam, info.Repo,
				buildIdParam, buildId, stepIdParam, stepId, stageIdParam, stageId, zap.Error(werr))
			WriteBadRequest(w, werr)
			return
		}
		w.WriteHeader(http.StatusNoContent)
	}
}
