package handler

import (
	"io"
	"net/http"
	"time"

	"github.com/wings-software/portal/product/log-service/logger"
	"github.com/wings-software/portal/product/log-service/store"
)

// HandleUpload returns an http.HandlerFunc that uploads
// a blob to the datastore.
func HandleUpload(store store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()

		key := r.FormValue("key")

		if err := store.Upload(ctx, key, r.Body); err != nil {
			WriteInternalError(w, err)
			logger.FromRequest(r).
				WithError(err).
				WithField("key", key).
				Errorln("api: cannot upload object")
			return
		}

		w.WriteHeader(http.StatusNoContent)
	}
}

// HandleUploadLink returns an http.HandlerFunc that generates
// a signed link to upload a blob to the datastore.
func HandleUploadLink(store store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx := r.Context()

		key := r.FormValue("key")
		expires := time.Hour

		link, err := store.UploadLink(ctx, key, expires)
		if err != nil {
			WriteInternalError(w, err)
			logger.FromRequest(r).
				WithError(err).
				WithField("key", key).
				Errorln("api: cannot generate upload url")
			return
		}

		WriteJSON(w, struct {
			Link    string        `json:"link"`
			Expires time.Duration `json:"expires"`
		}{link, expires}, 200)
	}
}

// HandleDownload returns an http.HandlerFunc that downloads
// a blob fromt the datastore and copies to the http.Response.
func HandleDownload(store store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		h := w.Header()
		h.Set("Access-Control-Allow-Origin", "*")
		ctx := r.Context()

		key := r.FormValue("key")

		out, err := store.Download(ctx, key)
		if out != nil {
			defer out.Close()
		}
		if err != nil {
			WriteNotFound(w, err)
			logger.FromRequest(r).
				WithError(err).
				Debugln("api: cannot download the object")
		} else {
			io.Copy(w, out)
		}
	}
}

// HandleDownloadLink returns an http.HandlerFunc that generates
// a signed link to download a blob to the datastore.
func HandleDownloadLink(store store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		h := w.Header()
		h.Set("Access-Control-Allow-Origin", "*")
		ctx := r.Context()

		key := r.FormValue("key")
		expires := time.Hour

		link, err := store.DownloadLink(ctx, key, expires)
		if err != nil {
			WriteInternalError(w, err)
			logger.FromRequest(r).
				WithError(err).
				WithField("key", key).
				Errorln("api: cannot generate download url")
			return
		}

		WriteJSON(w, struct {
			Link    string        `json:"link"`
			Expires time.Duration `json:"expires"`
		}{link, expires}, 200)
	}
}

// HandleDelete returns an http.HandlerFunc that deletes
// a blob from the datastore.
func HandleDelete(store store.Store) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		h := w.Header()
		h.Set("Access-Control-Allow-Origin", "*")
		ctx := r.Context()

		key := r.FormValue("key")

		if err := store.Delete(ctx, key); err != nil {
			WriteNotFound(w, err)
			logger.FromRequest(r).
				WithError(err).
				WithField("key", key).
				Errorln("api: cannot delete object")
			return
		}

		w.WriteHeader(http.StatusNoContent)
	}
}
