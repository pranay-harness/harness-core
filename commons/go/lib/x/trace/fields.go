package xtrace

import (
	"github.com/opentracing/opentracing-go"
	"github.com/opentracing/opentracing-go/ext"
	"github.com/opentracing/opentracing-go/log"
)

//LogError sets the error=true tag on the span and logs err as an "error" event
func LogError(span opentracing.Span, err error, fields ...log.Field) {
	ext.Error.Set(span, true)
	ef := []log.Field{
		log.String("event", "error"),
		log.Error(err),
	}
	ef = append(ef, fields...)
	span.LogFields(ef...)
}
