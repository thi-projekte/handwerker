import "@/assets/stylesheets/stylesheet.css";

export const LoginPage = () => {
  return (
    <div className="app">
      <div className="card login-card">
        <h1>Login</h1>
        <p className="text-secondary">Melde dich in deinem Account an</p>

        <div className="divider"></div>

        <input className="input-field" type="text" placeholder="Benutzername" />
        <input className="input-field" type="password" placeholder="Passwort" />

        <button className="button-primary login-btn">Einloggen</button>
        <button className="button-secondary">Registrieren</button>

        <div className="login-footer">
          <a href="#" className="text-secondary">
            Passwort vergessen?
          </a>
        </div>
      </div>
    </div>
  );
};
