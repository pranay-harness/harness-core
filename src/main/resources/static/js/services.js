'use strict';

/* Services */

var services = angular.module('wings.services', ['ngResource']);

services.factory('UserFactory', function ($resource) {
    return $resource('/wings/users', {}, {
        query: {
            method: 'GET',
            params: {},
            isArray: false
        }
    })
});
