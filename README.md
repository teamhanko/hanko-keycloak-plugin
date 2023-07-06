# Hanko Plugin for Keycloak

This is an extension to Keycloak which integrates passwordless authentication via Hanko.

For more information about Keycloak, please visit the [Keycloak homepage](https://www.keycloak.org/).

## Features

- Log in with either password, Hanko Authenticator (FIDO UAF) or WebAuthn (FIDO2).
- Enrollment of users via provided Keycloak admin API extension endpoints - "pre"-register FIDO2 roaming authenticators (security keys)
or initiate device binding using the Hanko Authenticator.

## Table of contents
- [Compatibility](#compatibility)
- [Installation](#installation)
- [Building](#building)
- [Running with Docker](#running-with-docker)
- Configuration
    - [Configure authentication flows](#configure-authentication-flows)
      - [Prerequisites](#prerequisites)
      - [Passwordless authentication flow](#passwordless-authentication-flow)
      - [Passwordless authentication flow with password fallback](#passwordless-authentication-flow-with-password-fallback)
- Using the Plugin API
    - [Preliminary notes](#preliminary-notes)
    - [OpenAPI Spec](#openapi-spec)
    - [Register devices](#register-devices)
      - [Register a WebAuthn device](#register-a-webauthn-device)
      - [Register a UAF device/Authenticator App](#register-a-uaf-deviceauthenticator-app)
      - [Get the status of a UAF device/Authenticator App request](#get-the-status-of-a-uaf-deviceauthenticator-app-request)
      - [Cancel a UAF device/Authenticator App request](#cancel-a-uaf-deviceauthenticator-app-request)
    - [Device management](#device-management)
        - [Retrieve devices](#retrieve-devices)
        - [Deregister devices](#deregister-devices)
          - [Deregister a WebAuthn device](#deregister-a-webauthn-device)
          - [Deregister a UAF device/Authenticator App device](#deregister-a-uaf-deviceauthenticator-app)
        - [Rename devices](#rename-devices)
          - [Rename a WebAuthn device](#rename-a-webauthn-device)
          - [Rename a UAF device/Authenticator App device](#rename-a-uaf-deviceauthenticator-app-device)


## Compatibility

The latest version of this plugin (3.0.0) has been tested with Keycloak 18.0.0

You can also build the plugin yourself, see section [Building](#building) for more details.

## Installation

1. Download the latest version artifacts (`hanko-keycloak-plugin.jar`) from the 
[GitHub releases page](https://github.com/teamhanko/hanko-keycloak-plugin/releases) or build them yourself 
(see section [Building](#building)).
2. Copy `hanko-keycloak-plugin.jar` to `<KEYCLOAK_ROOT>/providers/hanko-keycloak-plugin.jar`

## Building

Ensure you have JDK 8 (or newer), Maven 3.1.1 (or newer), node (14+) and Git installed:

```
java -version
mvn -version
node -v
git --version
```

Clone this repository:

```
git clone https://github.com/teamhanko/hanko-keycloak-plugin.git
cd hanko-keycloak-plugin
```

Build the plugin `jar` by running the following in the project root directory:

```
mvn clean package
```

You can find the `hanko-keycloak-plugin.jar` in the `target` folder of the `hanko-plugin-keycloak` directory.

## Running with Docker

The repository contains a Dockerfile that can be used to create an image and run Keycloak with the plugin in a container.
The Dockerfile uses the [keycloak](https://quay.io/repository/keycloak/keycloak) image. 

1. Build the `jar` file:

```
mvn clean package
```

2. Build the Docker image:

```
docker build -t <TAG> .
```

3. Run the container and create an initial admin account (to be able to open Keycloak on localhost map port 8080 
locally).

```
docker run -e KEYCLOAK_ADMIN=<USERNAME> -e KEYCLOAK_ADMIN_PASSWORD=<PASSWORD> -p 8080:8080 <TAG>
```

Keycloak uses an embedded H2 database per default. To run the container using a different database configuration see
the section **Database** for the [keycloak](https://quay.io/repository/keycloak/keycloak) image.

## Configure authentication flows

### Prerequisites

The plugin uses the API of the Hanko Authentication Service to perform 
authentication. Hanko API endpoints are protected and hence require an 
API Key ID/API Key pair to make authorized requests. In order to obtain an API Key ID/API Key pair: 

1. Go to `https://console.old.hanko.io`
2. Log in or register an account.
3. Once signed in, create an organization. If you already created an organization, select the organization.
4. Add a new relying party with the following properties:
    - Relying Party Name: choose any name you want
    - APP ID: **must** be a url with an `https` scheme.
5. Click "Add new relying party". Select the created relying party to get to the relying party dashboard.
6. On the relying party dashboard, select "General Settings" and then click "Add" in the "API Keys" panel.
This will generate an API Key and an API Key ID. You should store the API Key securely since it cannot be obtained once
you confirm and close the modal displayed after key generation.

> **Warning**
> Do not make the API Key publicly available.

The API Key ID/API Key pair will be used to configure Keycloak authentication flows (see the following sections for
details).

### Passwordless authentication flow

1. Log in to your Keycloak administration console.
2. Go to configuration section **Realm Settings**, open the Themes tab and select `keycloak-with-hanko` as your **login-theme**.
If you haven't already done so when [configuring a client](#configure-a-client), select `keycloak-with-hanko` as your **account-theme**.
   ![Select your theme](./docs/resources/change-login-theme.png)
3. Go to configuration section **Authentication**.
4. Select the **Browser** authentication flow.
5. Click **copy** in the top right corner of the table.
   ![Copy browser flow](./docs/resources/copy-browser-flow.png)
6. Give the new flow a meaningful name, for example **Browser flow with Hanko**.
   ![Rename new flow](./docs/resources/rename-copy-of-flow.png)
7. Select **Actions** and then **Delete** for the **Username Password Form**.
8. Select **Actions** and then **Delete** for the **Conditional OTP** flow.
   ![Delete actions](./docs/resources/delete-actions.png)
9. Select **Actions** for the **Browser Flow With Hanko Forms** and add the execution **Username Form**.
   ![Add Hanko actions](./docs/resources/add-execution-flows.png)
10. Select **Actions** for **Browser Flow With Hanko Forms** and add the execution **Hanko Multi Authenticator**.
Mark it as **required**.
11. Open the configuration of the **Hanko Multi Authenticator** flow by selecting **Actions** -> **Config**. Provide 
an alias for the configuration as well as your API Key ID and API Key secret. Save the configuration.
    ![Open UAF Config](./docs/resources/open-hanko-config.png)
12. Open the **Bindings** tab and change the **Browser Flow** to **Browser flow with Hanko**. Save the configuration.
    ![Change Binding](./docs/resources/change-binding.png)
    
> **Warning**
> Be careful when applying this authentication configuration to the master realm. Using this 
> configuration a user **must** have a device registered in order to properly log in. If you are a master realm admin 
> user without registered devices and apply this configuration as your bound browser flow, you can lock yourself out of
> the administration console. You can register devices using the enrollment feature. See 
> [Configure enrollment](#configure-enrollment) and [Using enrollment](#using-enrollment).

## Passwordless authentication flow with password fallback

If you still want a fallback to passwords as an alternative during login you can do so using a different authentication 
configuration. Perform steps (1)-(9) as described in the previous section, then continue as follows:

10. Select **Actions** for **Browser Flow With Hanko Forms** and select **Add Flow**.
    ![Add flow](./docs/resources/add-flow.png)
11. Give the flow a meaningful name, e.g. **Authentication**. Click **Save**.
    ![Name flow](./docs/resources/add-flow-name.png)
12. Mark the created **Authentication** flow as **required**.
    ![Mark flow as required](./docs/resources/add-flow-required.png)
13. Add another flow to the **Authentication** flow.
    ![Add flow to authentication flow](./docs/resources/add-flow-pwless.png)
14. Give the flow a meaningful name, e.g. **Passwordless**. Click **Save**.
    ![Name passwordless flow](./docs/resources/add-flow-pwless-name.png)
15. Mark the created **Passwordless** flow as **alternative**.
    ![Mark passwordless flow as alternative](./docs/resources/add-flow-pwless-alternative.png)
16. Add an execution to the **Passwordless** flow.
    ![Add execution to passwordless flow](./docs/resources/add-flow-pwless-add-execution.png)
17. Select **Hanko Multi Authenticator**. Click **Save**
    ![Select Hanko Multi Authenticator as execution](./docs/resources/add-flow-pwless-add-execution-multi.png)
18. Mark the **Hanko Multi Authenticator** execution as **required**.
    ![Mark Hanko Multi Authenticator as required](./docs/resources/add-flow-pwless-add-execution-multi-required.png)
19. Open the configuration of the **Hanko Multi Authenticator** execution by selecting **Actions** -> **Config**. Provide
    an alias for the configuration as well as your API Key ID and API Key secret. Save the configuration.
18. Add another flow to the **Authentication** flow (see step 13).
19. Give the flow a meaningful name, e.g. **Password**. Click **Save**.
    ![Name password flow](./docs/resources/add-flow-pw.png)
20. Mark the **Password** flow as **alternative**.
    ![Mark password flow as alternative](./docs/resources/add-flow-pw-alternative.png)
21. Add an execution to the **Password** flow.
    ![Add execution to password flow](./docs/resources/add-flow-pw-add-execution.png)
22. Select **Password Form**. Click **Save**.
    ![Select password form as execution](./docs/resources/add-flow-pw-add-execution-pf-form.png)
23. Mark the **Password Form** execution as **required**.
    ![Mark password form exeuction as required](./docs/resources/add-flow-pw-add-execution-pw-form-required.png)
24. Open the **Bindings** tab and change the **Browser Flow** to **Browser flow with Hanko**. Save the configuration.
    ![Change Binding](./docs/resources/change-binding.png)

Users should now be able to login using either Password, WebAuthn or Authenticator App.

## Using the Plugin API

### OpenAPI Spec

An OpenAPI specification for the endpoints can be found in the `/docs` directory (`spec.yaml`).

### Preliminary notes

> **Note** 
> In order to register a device for a user from a specific realm said realm  **must** have a
> **Hanko Multi Authenticator** execution configured in the Authentication configuration of the bound browser flow
> (see [Configure Authentication Flows](#configure-authentication-flows)).

> **Note**
> The session user/bearer of the access token **must** be able to manage users (role: `manage-users`)
> in the realm of the user they want to perform actions for through the API endpoints.

> **Note**  
> Note that the registration process is subject to timeout constraints. The default value for an
> initiated registration is 120 seconds. If you want to use registration initialization via QR code generation to - for
> example - deliver generated QR codes to users via postal mail, then this timeout is unfeasible. You can increase
> the timeout using the Hanko Developer Console (see [Prerequisites](#prerequisites)).
>1. Go to `https://console.hanko.io`.
>2. Log in to your account.
>3. Once signed in, click your profile name/icon in the top right and in the dropdown select "Change organization".
>4. Select the appropriate organization (i.e. the one you created in [Prerequisites](#prerequisites)).
>5. Select the appropriate relying party (i.e. the one you created in [Prerequisites](#prerequisites)).
>6. Select "FIDO Settings" in the menu on the left.
>7. Provide the desired value in the "Registration" input.
>8. Click "Update Changes".

### Register devices

#### Register a WebAuthn device

To register a WebAuthn device/security key you need to perform the following steps:

1. Initialize a reqistration by making a POST request to the **/{realmName}/{userId}/webauthn/register** endpoint, 
specifying the realm name (`realmName`) and the id of the user (`userId`) you want to register a device for as path 
parameters. Provide appropriate body for registering a cross-platform device (i.e. security key):
   
    ```js
      const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/webauthn/register`;

      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer <access_token>`
        },
        body: JSON.stringify({
          authenticatorAttachment: "cross-platform",
          requireResidentKey: "false",
          userVerification: "required",
        })
      })

      const hankoRequest = await response.json();
    ```
   
    The response is a `HankoRequest`. Its `status` should be `PENDING` at this point. The `request` property
    contains the stringified credential creation options to pass to the WebAuthn API. 

2. Pass the credential creation options to the WebAuthn API. The WebAuthn API requires some properties of the options 
to be in a binary format so a conversion step is required. You can use the [hanko-webauthn](https://www.npmjs.com/package/hanko-webauthn)
package to do so.

    1. First install the package or include the UMD module via script tag from CDN:

        ```shell
        npm install hanko-webauthn@1.2.0
        ```

        ```html
        <script src="https://cdn.jsdelivr.net/npm/hanko-webauthn@1.2.0/dist/umd/index.min.js"></script>
        ```
   
    2. Create a new instance of the `HankoWebAuthn` class:

        ```js
        import { HankoWebAuthn } from "hanko-webauthn";
        const hankoWebAuthn = new HankoWebAuthn();
        ```

        When using the UMD module, the class constructor is available under a global with the name `hankoWebAuthn`, 
        so creating an instance would be:

        ```html
        <script>
            const hankoWebAuthn = new hankoWebAuthn.HankoWebAuthn()
        </script>
        ```

    3. Pass the credential options contained in the `request` properties to the `createCredentials` method
    of the `HankoWebAuthn` instance:

        ```js
        const webAuthnResponse = await hankoWebAuthn.createCredentials(hankoRequest.request) 
        ```
       
3. Verify the WebAuthn response by making a POST request to the **/{realmName}/{userId}/webauthn/verify** endpoint, 
 again, specifying the realm name (realmName) and the id of the user (userId) you want to register a device for 
 as path parameters. Provide the `webAuthnResponse` (see step 2iii) as a body:
    
     ```js
     const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/webauthn/verify`;

     const response = await fetch(url, {
       method: "POST",
       headers: {
         "Content-Type": "application/json",
         "Authorization": `Bearer <access_token>`
       },
       body: JSON.stringify(webAuthnResponse)
     })

     const hankoRequest = await response.json();
     ```
        
     The response is again a `HankoRequest`. Its `status` property should be `OK` if the 
     verification was successful.

> **Note**
> You can also use the `hanko-webauthn` package to check whether the browser is WebAuthn capable
using the `isWebAuthnSupported`, `isSecurityKeySupported` methods on the `HankoWebAuthn` instance.

#### Register a UAF device/Authenticator App

To register a UAF device/Authenticator App you need to perform the following steps:

1. Initialize a reqistration by making a `POST` request to the **/{realmName}/{userId}/uaf/register** endpoint, 
specifying the realm name (`realmName`) and the id of the user (`userId`) you want to register a device for as path 
parameters.

    ```js
      const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/uaf/register`;

      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer <access_token>`
        }
      })

      const hankoRequest = await response.json();
    ```

    The response is a `HankoRequest`. Its `status` should be `PENDING` at this point.

2. To immediately display the QR Code associated with the request, extract the QR code source URL from the `links`
property of the `HankoRequest`:

    ```js
    const qrCodeURL = hankoRequest.links.find(link => link.rel === "qrcode").href;
    ```

    You can then use the `qrCodeURL` as the `src` for an `<img>`.

#### Get the status of a UAF device/Authenticator App request

To check the request status and retrieve the QR code (e.g. for display purposes after page reloads/at a later time - e.g. if registration
timeouts are set to a longer period of time), issue a `GET` request to the **/{realmName}/{userId}/uaf/status** endpoint:

```js
  const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/uaf/status`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer <access_token>`
    }
  })

  const hankoRequest = await response.json();
```
   
This fetches the last issued UAF/Authenticator App request issued for the user 
(see [Register a UAF device/Authenticator App](#register-a-uaf-deviceauthenticator-app), step 1). It returns a 
`HankoRequest`. The status is available in the status property of the response. The status is `PENDING` as long as there
has not been any attempt made to verify the request via Authenticator App. It is `OK` once the request has been verified
through the Authenticator App. It is `FAILED` if the request was explicitly cancelled 
(see [Cancelling a UAF/Authenticator App request](#cancelling-a-uafauthenticator-app-request)) or if an error 
occurred during verification.

> **Note**
> Unlike the WebAuthn registration process, there is no need to manually verify an authenticator response (i.e. similar to 
> the WebAuthn API response and the posting of the response payload to a verification endpoint). This verification step
> is done out-of-band by the Authenticator App after the user has scanned the request QR code and approved the request.
> Hence, to know whether a successful registration has occurred and to properly use/display this information in a client
> app, it is necessary to retrieve the status of the request as descripted in step 3.

#### Cancel a UAF device/Authenticator App request

To cancel a UAF/Authenticator App request, issue a `DELETE` request to the **/{realmName}/{userId}/uaf/cancel**
endpoint:

```js
  const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/uaf/cancel`;

  const response = await fetch(url, {
    method: "DELETE",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer <access_token>`
    }
  })

  const hankoRequest = await response.json();
```

The response is a `HankoRequest`. Its `status` property should be `FAILED` on successful cancellation.

### Device management

#### Retrieve devices

To retrieve all registered devices for a user, issue a `GET` request to the 
**/{realmName}/{userId}/devices** endpoint and provide the realm name (`realmName`) and the id of the user (`userId`)
you want to retrieve devices for as path parameters:

```js
  const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/devices`;

  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Accept": "application/json",
      "Authorization": `Bearer <access_token>`
    }
  })

  const devices = await response.json();
```

#### Deregister devices

##### Deregister a WebAuthn device

To deregister a WebAuthn device for a user, issue a `DELETE` request to the
**/{realmName}/{userId}/devices/webauthn/{deviceId}** endpoint and provide the realm name (`realmName`), the id of the user 
(`userId`) as well as the ID of the device (`deviceId`) you want to deregister as path parameters:

```js
  const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/devices/webauthn/{deviceId}`;

  const response = await fetch(url, {
    method: "DELETE",
    headers: {
      "Authorization": `Bearer <access_token>`
    }
  })

  const devices = await response.json();
```

##### Deregister a UAF device/Authenticator App

To deregister a UAF/Authenticator App device for a user, issue a `DELETE` request to the
**/{realmName}/{userId}/devices/uaf/{deviceId}** endpoint and provide the realm name (`realmName`), the id of the user
(`userId`) as well as the ID of the device (`deviceId`) you want to deregister as path parameters:

```js
  const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/devices/uaf/{deviceId}`;

  const response = await fetch(url, {
    method: "DELETE",
    headers: {
      "Authorization": `Bearer <access_token>`
    }
  })

  const devices = await response.json();
```

> **Note**
> Device deregistration removes only public credential material on the server side. This means that private 
> credential material will remain on an authenticator device unless a user explicitly deletes it from the authenticator.
> How credential material can be removed depends on the model of the authenticator: some manufacturers like Yubico offer
> dedicated software that provides a device settings UI, allowing for resetting the authenticator device. 
> Google Chrome's security settings also allow you interact with security keys 
> (e.g. via entering chrome://settings/securityKeys in the URL bar) and resetting the data on it.

#### Rename devices

##### Rename a WebAuthn device

To rename WebAuthn device for a user, issue a `PUT` request to the
**/{realmName}/{userId}/devices/webauthn/{deviceId}** endpoint and provide the realm name (`realmName`), the id of the user
(`userId`) as well as the ID of the device (`deviceId`) you want to rename as path parameters::

```js
  const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/devices/webauthn/{deviceId}`;

  const response = await fetch(url, {
    method: "PUT",
    headers: {
      "Authorization": `Bearer <access_token>`
    },
    body: JSON.stringify({
      newName: "My WebAuthn device"
    })
  })

  const renameResponse = await response.json();
```

When [retrieving a device](#retrieving-devices) the (new) name is available under the
`keyName` property.

##### Rename a UAF device/Authenticator App device

To rename a UAF/Authenticator App device for a user, issue a `PUT` request to the
**/{realmName}/{userId}/devices/uaf/{deviceId}** endpoint and provide the realm name (`realmName`), the id of the user
(`userId`) as well as the ID of the device (`deviceId`) you want to rename as path parameters::

```js
  const url = `<keycloak_server_url>/admin/realms/<your_client_app_realm>/hanko/{realmName}/{userId}/devices/uaf/{deviceId}`;

  const response = await fetch(url, {
    method: "PUT",
    headers: {
      "Authorization": `Bearer <access_token>`
    },
    body: JSON.stringify({
      newName: "My UAF device/Authenticator App"
    })
  })

  const renameResponse = await response.json();
```

When [retrieving a device](#retrieving-devices) the (new) name is available under the
`keyName` property.

## License

This product is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
