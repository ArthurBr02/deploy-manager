// Auth screens: login, forgot password, reset password

function LoginScreen() {
  return (
    <div className={`dm ${window.__dmDark?'dark':''}`} style={{flexDirection: 'column', background: 'var(--bg-muted)'}}>
      <div style={{flex: 1, display: 'grid', placeItems: 'center', padding: 24}}>
        <div style={{width: 380}}>
          <div style={{display: 'flex', alignItems: 'center', gap: 10, marginBottom: 28, justifyContent: 'center'}}>
            <div className="dm-brand-mark" style={{width: 32, height: 32, fontSize: 14}}>D</div>
            <div style={{fontWeight: 600, fontSize: 18}}>Deploy Manager</div>
          </div>
          <div className="dm-card" style={{padding: 28}}>
            <h2 style={{fontSize: 17, marginBottom: 4}}>Connexion</h2>
            <p className="dm-muted dm-small" style={{marginBottom: 20}}>
              Connectez-vous à votre espace de déploiement.
            </p>
            <div style={{marginBottom: 14}}>
              <label className="dm-label">Email</label>
              <input className="dm-input" defaultValue="arthur@example.com"/>
            </div>
            <div style={{marginBottom: 8}}>
              <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6}}>
                <label className="dm-label" style={{margin: 0}}>Mot de passe</label>
                <a style={{fontSize: 11.5, color: 'var(--accent)', textDecoration: 'none'}}>Oublié ?</a>
              </div>
              <input className="dm-input" type="password" defaultValue="••••••••••"/>
            </div>
            <button className="dm-btn primary lg" style={{width: '100%', justifyContent: 'center', marginTop: 18}}>
              Se connecter
            </button>
          </div>
          <p className="dm-muted dm-tiny" style={{textAlign: 'center', marginTop: 14}}>
            Application interne · accès réservé
          </p>
        </div>
      </div>
    </div>
  );
}

function ForgotScreen() {
  return (
    <div className={`dm ${window.__dmDark?'dark':''}`} style={{flexDirection: 'column', background: 'var(--bg-muted)'}}>
      <div style={{flex: 1, display: 'grid', placeItems: 'center', padding: 24}}>
        <div style={{width: 380}}>
          <div style={{display: 'flex', alignItems: 'center', gap: 10, marginBottom: 28, justifyContent: 'center'}}>
            <div className="dm-brand-mark" style={{width: 32, height: 32, fontSize: 14}}>D</div>
            <div style={{fontWeight: 600, fontSize: 18}}>Deploy Manager</div>
          </div>
          <div className="dm-card" style={{padding: 28}}>
            <h2 style={{fontSize: 17, marginBottom: 4}}>Mot de passe oublié</h2>
            <p className="dm-muted dm-small" style={{marginBottom: 20}}>
              Entrez votre email, nous vous enverrons un lien de réinitialisation (valide 1 heure).
            </p>
            <div style={{marginBottom: 18}}>
              <label className="dm-label">Email</label>
              <input className="dm-input" placeholder="vous@example.com"/>
            </div>
            <button className="dm-btn primary lg" style={{width: '100%', justifyContent: 'center'}}>
              Envoyer le lien
            </button>
            <div style={{textAlign: 'center', marginTop: 14}}>
              <a className="dm-small" style={{color: 'var(--text-muted)', textDecoration: 'none'}}>← Retour à la connexion</a>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function ResetScreen() {
  return (
    <div className={`dm ${window.__dmDark?'dark':''}`} style={{flexDirection: 'column', background: 'var(--bg-muted)'}}>
      <div style={{flex: 1, display: 'grid', placeItems: 'center', padding: 24}}>
        <div style={{width: 380}}>
          <div style={{display: 'flex', alignItems: 'center', gap: 10, marginBottom: 28, justifyContent: 'center'}}>
            <div className="dm-brand-mark" style={{width: 32, height: 32, fontSize: 14}}>D</div>
            <div style={{fontWeight: 600, fontSize: 18}}>Deploy Manager</div>
          </div>
          <div className="dm-card" style={{padding: 28}}>
            <h2 style={{fontSize: 17, marginBottom: 4}}>Nouveau mot de passe</h2>
            <p className="dm-muted dm-small" style={{marginBottom: 20}}>
              Choisissez un nouveau mot de passe sécurisé (12 caractères minimum).
            </p>
            <div style={{marginBottom: 14}}>
              <label className="dm-label">Nouveau mot de passe</label>
              <input className="dm-input" type="password" defaultValue="••••••••••"/>
            </div>
            <div style={{marginBottom: 18}}>
              <label className="dm-label">Confirmation</label>
              <input className="dm-input" type="password" defaultValue="••••••••••"/>
            </div>
            <div style={{background: 'var(--bg-subtle)', border: '1px solid var(--border)', borderRadius: 6, padding: '8px 10px', marginBottom: 18, fontSize: 11.5, color: 'var(--text-muted)'}}>
              <div style={{display: 'flex', alignItems: 'center', gap: 6}}>
                <span style={{color: 'var(--status-success)', display: 'inline-flex', width: 12, height: 12}}>{Icon.check}</span>
                12 caractères minimum
              </div>
              <div style={{display: 'flex', alignItems: 'center', gap: 6, marginTop: 3}}>
                <span style={{color: 'var(--status-success)', display: 'inline-flex', width: 12, height: 12}}>{Icon.check}</span>
                Au moins une majuscule et un chiffre
              </div>
            </div>
            <button className="dm-btn primary lg" style={{width: '100%', justifyContent: 'center'}}>
              Mettre à jour
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { LoginScreen, ForgotScreen, ResetScreen });
