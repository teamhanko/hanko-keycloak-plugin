require('./utils/publicPath')

import * as React from 'react'
import * as ReactDOM from 'react-dom'
import { App } from './containers/App'

import * as Keycloak from 'keycloak-js'

import { NotLoggedIn } from './containers/NotLoggedIn'
import { IntlProvider, addLocaleData } from 'react-intl'
import * as moment from 'moment'

const development = process.env.NODE_ENV !== 'production'

if (development) {
  require('./styles/kvsh.scss')
}

const keycloakUrl =
  (window as any).keycloakUrl !== undefined
    ? (window as any).keycloakUrl
    : `${process.env.KEYCLOAK_URL}/auth`

const realm =
  (window as any).realmName !== undefined
    ? (window as any).realmName
    : `${process.env.KEYCLOAK_REALM}`

const keycloak = Keycloak({
  url: keycloakUrl,
  realm: realm,
  clientId: 'hanko-account'
})

const language =
  (navigator.languages && navigator.languages[0]) ||
  navigator.language ||
  navigator.userLanguage
const languageWithoutRegionCode = language.toLowerCase().split(/[_-]+/)[0]

// load our messages
const localeData = require('./translations/aggregated/data.json')

const en = require('react-intl/locale-data/en')
const de = require('react-intl/locale-data/de')
addLocaleData([...en, ...de])

const messages =
  localeData[languageWithoutRegionCode] || localeData[language] || localeData.en

moment.locale(languageWithoutRegionCode)

keycloak
  .init({ onLoad: 'login-required' })
  .success(_ => {
    ReactDOM.render(
      <IntlProvider locale={language} messages={messages}>
        <App keycloak={keycloak} />
      </IntlProvider>,
      document.getElementById('root') as HTMLElement
    )
  })
  .error(() => {
    ReactDOM.render(
      <IntlProvider locale={language} messages={messages}>
        <NotLoggedIn keycloak={keycloak} />
      </IntlProvider>,
      document.getElementById('root') as HTMLElement
    )
  })
