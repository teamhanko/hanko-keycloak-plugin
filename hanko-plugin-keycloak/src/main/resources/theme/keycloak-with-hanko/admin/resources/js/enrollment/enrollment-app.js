module.config(['$routeProvider', function ($routeProvider) {
    $routeProvider
        .when('/realms/:realm/users/:user/enrollment', {
            templateUrl : resourceUrl + '/partials/enrollment.html',
            resolve : {
                realm : function(RealmLoader) {
                    return RealmLoader();
                },
                user : function(UserLoader) {
                    return UserLoader();
                }
            },
            controller : 'EnrollmentCtrl'
        })
    ;
}]);