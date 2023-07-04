<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        <#if loginMethod = "UAF">
            ${msg("signInAuthenticator")}
        <#elseif loginMethod = "WEBAUTHN">
            ${msg("signInWebAuthn")}
        </#if>
    <#elseif section = "header">
        <#if loginMethod = "UAF">
            ${msg("signInAuthenticator")}
        <#elseif loginMethod = "WEBAUTHN">
            ${msg("signInWebAuthn")}
        </#if>
    <#elseif section = "form">
        <#if loginMethod = "UAF">
            <p>${msg("signInDescriptionAuthenticator")}</p>

            <img src="${url.resourcesPath}/img/login-hanko.png" width="120" style="display: block; margin: 50px auto">

            <form action="${url.loginAction}" style="display:hidden" class="${properties.kcFFormClass!}"
                  id="kc-hanko-login-form"
                  method="post">
                <input type="hidden" name="loginMethod" value="UAF" />
            </form>

            <#include "hanko-multi-login-links.ftl">

            <script src="${url.resourcesPath}/js/hanko.js"></script>
            <script>
                const awaitLoginComplete = () => {
                    fetchWithTimeout(() => fetch('${baseUrl}admin/realms/${realm.name}/hanko/request/${requestId}'))
                            .then(response => response.json())
                            .catch(error => {
                                console.error('Error:', error)
                                setTimeout(function () {
                                    awaitLoginComplete();
                                }, 1000);
                            }).then(response => {
                        if (response.status === "PENDING") {
                            setTimeout(function () {
                                awaitLoginComplete();
                            }, 500);
                        } else {
                            document.getElementById('kc-hanko-login-form').submit();
                        }
                    });
                };
                window.onload = awaitLoginComplete;
            </script>
        <#elseif loginMethod = "WEBAUTHN">
            <p>${msg("signInDescriptionWebAuthn")}</p>

            <div class="flexrow">
                <div class="imgwrapper">
                    <img src="${url.resourcesPath}/img/windows-hello.png">
                </div>
                <div class="imgwrapper">
                    <img src="${url.resourcesPath}/img/yubikey.png">
                </div>
            </div>

            <form action="${url.loginAction}" style="display:hidden" class="${properties.kcFFormClass!}"
                  id="kc-hanko-login-form"
                  method="post">
                <input type="hidden" name="hankoresponse" id="hankoresponse" />
                <input type="hidden" name="loginMethod" value="WEBAUTHN" />
            </form>
            <script>
                var fidoRequest = JSON.parse('${request?no_esc}');

                function convertToBinary (dataURI) {
                  var raw = window.atob(dataURI)
                  var rawLength = raw.length
                  var array = new Uint8Array(new ArrayBuffer(rawLength))

                  for (let i = 0; i < rawLength; i++) {
                    array[i] = raw.charCodeAt(i)
                  }
                  return array
                }

                function arrayBufferToBase64Url(buf) {
                  var binary = ''
                  var bytes = new Uint8Array(buf)
                  var len = bytes.byteLength
                  for (var i = 0; i < len; i++) {
                    binary += String.fromCharCode(bytes[i])
                  }
                  return window
                    .btoa(binary)
                    .replace(/\//g, '_')
                    .replace(/\+/g, '-')
                }

                function base64UrlToArrayBuffer(data) {
                  let input = data.replace(/-/g, '+')
                    .replace(/_/g, '/')

                  return convertToBinary(input)
                }

                function decodeAuthenticationRequest(request) {
                  var newRequest = request
                  newRequest.challenge = base64UrlToArrayBuffer(request.challenge)
                  newRequest.allowCredentials = request.allowCredentials.map(function (cred) {
                    return {
                      id: base64UrlToArrayBuffer(cred.id),
                      type: cred.type,
                      transports: cred.transports
                    }
                  })

                  return newRequest
                }

                function encodeAuthenticationResult(data) {
                  var d = {}
                  d.response = {}
                  d.rawId = arrayBufferToBase64Url(data.rawId)
                  d.id = data.id
                  d.type = data.type
                  d.response.clientDataJSON = arrayBufferToBase64Url(data.response.clientDataJSON)
                  d.response.authenticatorData = arrayBufferToBase64Url(data.response.authenticatorData)
                  d.response.signature = arrayBufferToBase64Url(data.response.signature)
                  d.response.userHandle = arrayBufferToBase64Url(data.response.userHandle)

                  return d
                }

                var getCredentialOptions = decodeAuthenticationRequest(fidoRequest)

                navigator.credentials.get({publicKey: getCredentialOptions})
                        .then(function (credentialResult) {

                            var hankoResponse = encodeAuthenticationResult(credentialResult)

                            console.log(hankoResponse);
                            document.getElementById('hankoresponse').value = JSON.stringify(hankoResponse);
                            document.getElementById('kc-hanko-login-form').submit();
                        })
                        .catch(function (reason) {
                            console.error(reason)
                        });
            </script>
            <#include "hanko-multi-login-links.ftl">
        <#else>
            <p>${msg("signInNoDevicesRegistered")}</p>
        </#if>

    </#if>
</@layout.registrationLayout>
