<html>

<head>
  <title>Manage Account</title>
  <meta charset="utf-8">
  <script>
    window.keycloakUrl = "${keycloakUrl!}";
    window.realmId = "${keycloakRealmId!}";
    window.realmName = "${keycloakRealm!}";
    window.resourceBaseUrl = '${url.resourcesPath}/js/';
    <#--window.requires2fa = "${requires2fa}";-->
  </script>
  <link href="${url.resourcesPath}/css/main.css" rel="stylesheet" />
  <link href="https://fonts.googleapis.com/css?family=Montserrat:100,100i,200,200i,300,300i,400,400i,500,500i,600,600i,700,700i,800,800i,900,900i&subset=latin-ext" rel="stylesheet">
</head>

<body style="height: 100%">
  <noscript>
    You need to enable JavaScript to run this app.
  </noscript>
  <div id="root"></div>
</body>

</html>