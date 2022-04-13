module.controller('EnrollmentCtrl', function($scope, $http, realm, user, $route, User, Notifications, Dialog, EnrollmentService) {

    $scope.realm = realm;
    $scope.user = angular.copy(user);

    var hankoWebAuthn = new window.hankoWebAuthn.HankoWebAuthn();

    loadLastUafEnrollmentRequest($scope.auth.user, user, realm);

    function loadLastUafEnrollmentRequest(currentUser, userToEnroll, userToEnrollRealm) {
        var requestId = EnrollmentService.getLastUafRequestId(userToEnroll);

        if (requestId) {
            EnrollmentService.getUafRequest(currentUser, requestId, userToEnrollRealm).then(function(result) {
                $scope.lastUafRequest = result.data;
                $scope.lastUafRequestQrCodeLink = result.data.links.find(link => link.rel === 'qrcode').href;
            }).catch(function(error) {
                Notifications.error("Could not load pending registration request: " + error);
            });
        }
    }

    $scope.addAuthenticator = function(currentUser, userToEnroll, userToEnrollRealm) {
        EnrollmentService.registerUaf(currentUser, userToEnroll, userToEnrollRealm)
            .then(function(hankoCreateRequestResponse) {
                if (hankoCreateRequestResponse.data.status === 'PENDING') {
                    Notifications.success("Registration request created");
                    $route.reload();
                } else {
                    Notifications.error("Could not create registration request");
                }
            })
            .catch(function(error) {
                Notifications.error("Could not create registration request: " + error);
            });
    };

    $scope.cancelAddAuthenticator = function(currentUser, userToEnroll, userToEnrollRealm) {
        Dialog.confirm('Cancel pending request', 'Are you sure you want to cancel the pending request?', function() {
            EnrollmentService.cancelUaf(currentUser, userToEnroll, userToEnrollRealm).then(function() {
                Notifications.success("Pending request canceled!");
                $route.reload();
            }, function(error) {
                Notifications.error("Error while canceling the request: " + error);
            });
        });
    };

    $scope.addSecurityKey = function(currentUser, userToEnroll, userToEnrollRealm) {
        var webAuthnRequestId;

        EnrollmentService.registerWebAuthn(currentUser, userToEnroll, userToEnrollRealm)
            .then(function(result) {
                webAuthnRequestId = result.data.id;
                return hankoWebAuthn.createCredentials(result.data.request);
            })
            .then(function(webAuthnResponse) {
                return EnrollmentService.verifyWebAuthn(currentUser, webAuthnResponse, userToEnrollRealm, webAuthnRequestId);
            })
            .then(function() {
                Notifications.success("Security key registration successful")
            })
            .catch(function(error) {
                Notifications.error("Could not register security key: " + error)
            });
    }
});