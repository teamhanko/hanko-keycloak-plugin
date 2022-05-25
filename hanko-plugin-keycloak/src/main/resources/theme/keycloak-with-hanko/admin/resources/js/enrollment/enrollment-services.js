module.factory('EnrollmentService', ['$http', function($http) {
    var enrollment = {};

    enrollment.registerUaf = function(currentUser, userToEnroll, userToEnrollRealm) {
        return $http({
            method: 'POST',
            url: authUrl + '/realms/' + currentUser.realm + '/enrollment/' + userToEnrollRealm.id + '/uaf',
            params: { userId: userToEnroll.id, realmId: userToEnrollRealm.id },
            data: {}
        })
    };

    enrollment.registerWebAuthn = function(currentUser, userToEnroll, userToEnrollRealm) {
        return $http({
            method: 'POST',
            url: authUrl + '/realms/' + currentUser.realm + '/enrollment/' + userToEnrollRealm.id + '/webauthn',
            params: { userId: userToEnroll.id },
            data: {
                authenticatorAttachment: 'cross-platform',
                requireResidentKey: false,
                userVerification: 'required'
            }
        })
    };

    enrollment.cancelUaf = function(currentUser, userToEnroll, userToEnrollRealm) {
        var requestId = enrollment.getLastUafRequestId(userToEnroll);

        return $http({
            method: 'DELETE',
            url: authUrl + '/realms/' + currentUser.realm + '/enrollment/' + userToEnrollRealm.id + '/uaf/' + requestId,
            params: { userId: userToEnroll.id }
        })
    };

    enrollment.verifyWebAuthn = function(currentUser, webAuthnResponse, userToEnrollRealm, requestId) {
        return $http({
            method: 'PUT',
            url: authUrl + '/realms/' + currentUser.realm + '/enrollment/' + userToEnrollRealm.id + '/webauthn/' + requestId,
            data: webAuthnResponse
        })
    };

    enrollment.getUafRequest = function(currentUser, requestId, userToEnrollRealm) {
        return $http({
            method: 'GET',
            url: authUrl + '/realms/' + currentUser.realm + '/enrollment/' + userToEnrollRealm.id +'/uaf/' + requestId
        })
    };

    enrollment.getLastUafRequestId = function(user) {
        var requestId;

        if (user.attributes) {
            requestId = user.attributes['HANKO_ENROLLMENT_UAF_REQUEST_ID'];
        }

        if (requestId && requestId[0] !== undefined && requestId[0] !== '') {
            return requestId[0];
        } else {
            return undefined;
        }
    };

    return enrollment
}]);