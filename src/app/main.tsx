import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';
import keycloak from './keycloak';

keycloak.init({ onLoad: 'login-required' }).then((authenticated) => {
  if (authenticated) {
    ReactDOM.createRoot(document.getElementById('root')!).render(
      <React.StrictMode>
        <App />
      </React.StrictMode>
    );
  }
});