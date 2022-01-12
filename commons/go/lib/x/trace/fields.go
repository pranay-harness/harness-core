// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
