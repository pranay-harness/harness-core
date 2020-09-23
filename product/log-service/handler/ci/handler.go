package cihandler

import (
	"io"
	"net/http"

	"github.com/wings-software/portal/product/log-service/store"
	"github.com/wings-software/portal/product/log-service/stream"

	"github.com/go-chi/chi"
)

// Handler returns an http.Handler that exposes the
// service resources.
func AddRoutes(stream stream.Stream, store store.Store, h chi.Router) {

	h.Route("/ci/streams", func(r chi.Router) {
		r.Get("/", HandleInfo(stream))
	})

	// TODO: Change this to query params if required
	h.Route("/api/accounts/{account_id}/orgs/{org_id}/projects/{project_id}/builds/{build_id}/logs/{stage_id}/{step_id}/stream", func(r chi.Router) {
		r.Post("/", HandleOpen(stream))
		r.Delete("/", HandleClose(stream))
		r.Put("/", HandleWrite(stream))
		r.Get("/", HandleTail(stream))
	})

	h.Route("/api/accounts/{account_id}/orgs/{org_id}/projects/{project_id}/builds/{build_id}/logs/{stage_id}/{step_id}/blob", func(r chi.Router) {
		r.Post("/", HandleUpload(store, stream))
		r.Delete("/", HandleDelete(store))
		r.Get("/", HandleDownload(store))
		r.Post("/link/upload", HandleUploadLink(store))
		r.Post("/link/download", HandleDownloadLink(store))
	})

	h.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		io.WriteString(w, "OK")
	})
}

// ParseKeyFromURL uniquely identifies a key to upload as well as subscribe to.
func ParseKeyFromURL(r *http.Request) string {
	accountId := chi.URLParam(r, "account_id")
	orgId := chi.URLParam(r, "org_id")
	projectId := chi.URLParam(r, "project_id")
	buildId := chi.URLParam(r, "build_id")
	stageId := chi.URLParam(r, "stage_id")
	stepId := chi.URLParam(r, "step_id")
	return "/" + accountId + "/" + orgId + "/" + projectId + "/" + buildId + "/" + stageId + "/" + stepId
}
