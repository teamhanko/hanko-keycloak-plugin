export const fetchApi = (
  keycloak: Keycloak.KeycloakInstance,
  path: string,
  method: string | undefined = undefined,
  body: any | undefined = undefined
) => {
  return new Promise<any>((success, fail) => {
    const keycloakUrl = (window as any).keycloakUrl
      ? (window as any).keycloakUrl
      : `${process.env.KEYCLOAK_URL}`

    const realm = (window as any).realmName
      ? (window as any).realmName
      : `${process.env.KEYCLOAK_REALM}`

    keycloak.updateToken(30).success(_ => {
      const url = `${keycloakUrl}realms/${realm}${path}`

      const headers = (body
        ? {
            authorization: `Bearer ${keycloak.token}`,
            'Content-Type': 'application/json'
          }
        : {
            authorization: `Bearer ${keycloak.token}`
          }) as any

      const options = {
        method: method ? method : 'GET',
        headers: headers,
        body: body ? JSON.stringify(body) : null
      }

      fetch(url, options)
        .then(response => {
          return response.json()
        })
        .then(json => success(json))
        .catch(_ => success(undefined))
    })
  })
}
