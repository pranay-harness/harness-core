// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

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
