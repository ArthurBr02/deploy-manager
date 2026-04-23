// Admin screens: users list, user detail + permissions, host create/edit, import Ansible, settings

const SAMPLE_USERS = [
  { id: 'u1', name: 'Arthur Br.',   email: 'arthur@arthurbr02.fr',  role: 'ADMIN', initials: 'AB', last: "aujourd'hui, 09:14", hosts: 24 },
  { id: 'u2', name: 'Sophie Martin', email: 'sophie@arthurbr02.fr', role: 'USER',  initials: 'SM', last: 'il y a 2 h',          hosts: 6 },
  { id: 'u3', name: 'Julien Legrand', email: 'julien@arthurbr02.fr', role: 'USER',  initials: 'JL', last: 'hier',                hosts: 4 },
  { id: 'u4', name: 'Emma Dubois',   email: 'emma@arthurbr02.fr',    role: 'USER',  initials: 'ED', last: 'il y a 5 j',          hosts: 2 },
  { id: 'u5', name: 'Thomas Noël',   email: 'thomas@arthurbr02.fr',  role: 'ADMIN', initials: 'TN', last: 'il y a 1 h',          hosts: 24 },
];

function AdminUsers() {
  return (
    <AppShell active="users" role="admin"
              title="Utilisateurs"
              topRight={<>
                <div style={{position: 'relative'}}>
                  <span style={{position: 'absolute', left: 10, top: 8, width: 14, height: 14, color: 'var(--text-subtle)'}}>{Icon.search}</span>
                  <input className="dm-input" placeholder="Rechercher…" style={{paddingLeft: 30, width: 220}}/>
                </div>
                <button className="dm-btn primary">{Icon.plus}Nouvel utilisateur</button>
              </>}>
      <p className="dm-small dm-muted" style={{marginBottom: 16}}>
        Gérez les comptes et les droits d'accès aux hôtes.
      </p>
      <div className="dm-card" style={{overflow: 'hidden'}}>
        <table className="dm-table">
          <thead>
            <tr>
              <th>Utilisateur</th>
              <th>Rôle</th>
              <th>Hôtes accessibles</th>
              <th>Dernière connexion</th>
              <th style={{textAlign: 'right'}}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {SAMPLE_USERS.map(u => (
              <tr key={u.id}>
                <td>
                  <div style={{display: 'flex', alignItems: 'center', gap: 10}}>
                    <div className="dm-avatar" style={{background: u.role === 'ADMIN'
                      ? 'linear-gradient(135deg, var(--accent), #7c3aed)'
                      : 'linear-gradient(135deg, #64748b, #475569)'}}>{u.initials}</div>
                    <div>
                      <div style={{fontWeight: 500}}>{u.name}</div>
                      <div className="dm-tiny dm-muted">{u.email}</div>
                    </div>
                  </div>
                </td>
                <td>
                  {u.role === 'ADMIN'
                    ? <span className="dm-badge" style={{background: 'var(--accent-subtle)', color: 'var(--accent-text)', borderColor: '#d8dcff'}}>Administrateur</span>
                    : <span className="dm-badge neutral">Utilisateur</span>}
                </td>
                <td>{u.role === 'ADMIN' ? <span className="dm-muted">Tous (24)</span> : `${u.hosts} hôte${u.hosts > 1 ? 's' : ''}`}</td>
                <td className="dm-muted">{u.last}</td>
                <td style={{textAlign: 'right'}}>
                  <div style={{display: 'flex', justifyContent: 'flex-end', gap: 4}}>
                    <button className="dm-btn sm ghost"><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.key}</span>Reset mdp</button>
                    <button className="dm-btn sm"><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.edit}</span>Modifier</button>
                    <button className="dm-btn sm ghost" style={{padding: 4}}><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.more}</span></button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AppShell>
  );
}

function AdminUserDetail() {
  const u = SAMPLE_USERS[1];
  return (
    <AppShell active="users" role="admin"
              crumbs={['Utilisateurs', u.name]}
              topRight={<>
                <button className="dm-btn"><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.key}</span>Réinitialiser mdp</button>
                <button className="dm-btn danger"><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.trash}</span>Supprimer</button>
              </>}>
      <div style={{display: 'grid', gridTemplateColumns: '320px 1fr', gap: 20}}>
        <div className="dm-card" style={{padding: 20}}>
          <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8, marginBottom: 18}}>
            <div className="dm-avatar" style={{width: 64, height: 64, fontSize: 22, background: 'linear-gradient(135deg, #64748b, #475569)'}}>{u.initials}</div>
            <div style={{fontSize: 16, fontWeight: 600}}>{u.name}</div>
            <div className="dm-small dm-muted">{u.email}</div>
            <span className="dm-badge neutral">Utilisateur</span>
          </div>

          <div className="dm-hr"/>

          <div style={{display: 'flex', flexDirection: 'column', gap: 12}}>
            <div>
              <label className="dm-label">Prénom</label>
              <input className="dm-input" defaultValue="Sophie"/>
            </div>
            <div>
              <label className="dm-label">Nom</label>
              <input className="dm-input" defaultValue="Martin"/>
            </div>
            <div>
              <label className="dm-label">Email</label>
              <input className="dm-input" defaultValue={u.email}/>
            </div>
            <div>
              <label className="dm-label">Rôle</label>
              <select className="dm-select" defaultValue="USER">
                <option value="USER">Utilisateur</option>
                <option value="ADMIN">Administrateur</option>
              </select>
              <p className="dm-hint">Un administrateur a tous les droits sur les hôtes.</p>
            </div>
            <button className="dm-btn primary" style={{marginTop: 4, justifyContent: 'center'}}>Enregistrer</button>
          </div>

          <div className="dm-hr"/>
          <div className="dm-tiny dm-muted">
            Créé le 12 janv. 2026 · Dernière connexion il y a 2 h
          </div>
        </div>

        <div>
          <div className="dm-card">
            <div className="dm-card-head">
              <div>
                <h3>Accès aux hôtes</h3>
                <div className="dm-tiny dm-muted" style={{marginTop: 2}}>Choisissez finement les droits de <b>{u.name.split(' ')[0]}</b> sur chaque hôte.</div>
              </div>
              <button className="dm-btn sm">{Icon.plus}Ajouter un hôte</button>
            </div>
            <table className="dm-table">
              <thead>
                <tr>
                  <th>Hôte</th>
                  <th style={{width: 130, textAlign: 'center'}}>Peut déployer</th>
                  <th style={{width: 130, textAlign: 'center'}}>Peut modifier</th>
                  <th style={{width: 40}}/>
                </tr>
              </thead>
              <tbody>
                {SAMPLE_HOSTS.slice(0, 6).map((h, i) => {
                  const canDeploy = i !== 3;
                  const canEdit = i === 0 || i === 2;
                  return (
                    <tr key={h.id}>
                      <td>
                        <div style={{display: 'flex', alignItems: 'center', gap: 8}}>
                          <span style={{width: 14, height: 14, color: 'var(--text-subtle)', display: 'inline-flex'}}>{Icon.server}</span>
                          <div>
                            <div style={{fontWeight: 500}}>{h.name}</div>
                            <div className="dm-tiny dm-muted dm-mono">{h.ip}</div>
                          </div>
                        </div>
                      </td>
                      <td style={{textAlign: 'center'}}>
                        <Toggle on={canDeploy}/>
                      </td>
                      <td style={{textAlign: 'center'}}>
                        <Toggle on={canEdit}/>
                      </td>
                      <td><button className="dm-btn ghost sm" style={{padding: 4}}><span style={{width: 12, height: 12, display: 'inline-flex', color: 'var(--status-failure)'}}>{Icon.trash}</span></button></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div style={{marginTop: 18}}>
            <h3 style={{fontSize: 13, marginBottom: 8}}>Activité récente</h3>
            <div className="dm-card" style={{overflow: 'hidden'}}>
              <table className="dm-table">
                <tbody>
                  {SAMPLE_DEPLOYMENTS.slice(0, 4).map(d => (
                    <tr key={d.id}>
                      <td className="mono" style={{width: 100}}>#{d.id}</td>
                      <td>{d.type === 'DEPLOY' ? 'Déployé' : d.type === 'GENERATE' ? 'Généré' : 'Livré'} <b>vpn</b></td>
                      <td><StatusBadge status={d.status}/></td>
                      <td className="dm-muted" style={{textAlign: 'right'}}>{d.at}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
  );
}

function Toggle({ on }) {
  return (
    <span style={{
      display: 'inline-flex', width: 30, height: 18,
      borderRadius: 999, background: on ? 'var(--accent)' : 'var(--border-strong)',
      padding: 2, transition: 'background .15s', cursor: 'pointer'
    }}>
      <span style={{
        width: 14, height: 14, borderRadius: '50%', background: '#fff',
        transform: on ? 'translateX(12px)' : 'translateX(0)',
        transition: 'transform .15s', boxShadow: '0 1px 2px rgba(0,0,0,.2)'
      }}/>
    </span>
  );
}

function AdminUserCreated() {
  return (
    <div className={`dm ${window.__dmDark?'dark':''}`} style={{position: 'relative'}}>
      <Sidebar active="users" role="admin"/>
      <div className="dm-main">
        <Topbar crumbs={['Utilisateurs', 'Nouvel utilisateur']}/>
        <div className="dm-content dm-scroll" style={{opacity: .4}}>
          <div className="dm-card" style={{padding: 20, maxWidth: 540}}>
            <h3 style={{marginBottom: 12}}>Nouvel utilisateur</h3>
            <div className="dm-col" style={{gap: 12}}>
              <input className="dm-input" defaultValue="Emma Dubois"/>
              <input className="dm-input" defaultValue="emma@arthurbr02.fr"/>
            </div>
          </div>
        </div>
      </div>
      <div className="dm-overlay">
        <div className="dm-modal" style={{width: 480}}>
          <div className="dm-modal-head">
            <div style={{display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4}}>
              <span style={{width: 20, height: 20, color: 'var(--status-success)', display: 'inline-flex'}}>{Icon.check}</span>
              <h3 style={{fontSize: 15}}>Compte créé</h3>
            </div>
            <p className="dm-small dm-muted">
              Communiquez ce mot de passe temporaire à Emma. Il ne sera plus affiché par la suite.
            </p>
          </div>
          <div className="dm-modal-body">
            <label className="dm-label">Mot de passe temporaire</label>
            <div style={{display: 'flex', gap: 6}}>
              <input className="dm-input mono" readOnly value="Xq8-tR4n!uPwK9aZ"/>
              <button className="dm-btn"><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.copy}</span>Copier</button>
            </div>
            <div style={{background: 'var(--status-progress-bg)', border: '1px solid var(--status-progress-border)', borderRadius: 6, padding: 10, marginTop: 12, display: 'flex', gap: 8, color: 'var(--status-progress)', fontSize: 11.5}}>
              <span style={{width: 14, height: 14, display: 'inline-flex', flexShrink: 0}}>{Icon.alert}</span>
              <div style={{color: 'var(--text)'}}>
                <b>Affiché une seule fois.</b> L'utilisateur devra le changer à sa première connexion.
              </div>
            </div>
          </div>
          <div className="dm-modal-foot">
            <button className="dm-btn primary">J'ai noté le mot de passe</button>
          </div>
        </div>
      </div>
    </div>
  );
}

function AdminHostCreate() {
  return (
    <AppShell active="hosts" role="admin"
              crumbs={['Hôtes', 'Nouvel hôte']}>
      <div style={{maxWidth: 680}}>
        <div className="dm-card" style={{padding: 22, marginBottom: 18}}>
          <h3 style={{marginBottom: 4}}>Informations de l'hôte</h3>
          <p className="dm-small dm-muted" style={{marginBottom: 16}}>Renseignez l'identité et les points d'accès du serveur.</p>
          <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14}}>
            <div>
              <label className="dm-label">Nom de l'hôte</label>
              <input className="dm-input mono" placeholder="ex: vpn"/>
              <p className="dm-hint">Utilisé dans <code className="dm-mono">{'{host}'}</code>.</p>
            </div>
            <div>
              <label className="dm-label">Adresse IP</label>
              <input className="dm-input mono" placeholder="10.42.1.12"/>
            </div>
            <div style={{gridColumn: '1 / -1'}}>
              <label className="dm-label">Domaine</label>
              <input className="dm-input mono" placeholder="vpn.arthurbr02.fr"/>
              <p className="dm-hint">Obligatoire pour les créations manuelles.</p>
            </div>
          </div>
        </div>

        <div className="dm-card" style={{padding: 22, marginBottom: 18}}>
          <h3 style={{marginBottom: 4}}>Commandes personnalisées</h3>
          <p className="dm-small dm-muted" style={{marginBottom: 14}}>
            Laissez vide pour utiliser la commande globale par défaut. Variables : <code className="dm-mono">{'{host}'}</code>, <code className="dm-mono">{'{ip}'}</code>, <code className="dm-mono">{'{domain}'}</code>.
          </p>
          <div style={{display: 'flex', flexDirection: 'column', gap: 14}}>
            <div>
              <label className="dm-label" style={{display: 'flex', alignItems: 'center', gap: 6}}>
                <span style={{width: 13, height: 13, color: 'var(--accent)', display: 'inline-flex'}}>{Icon.rocket}</span>
                Commande de déploiement
              </label>
              <input className="dm-input mono" placeholder="sh /root/{host}/liv.sh"/>
              <p className="dm-hint">Par défaut : <code className="dm-mono">sh /root/{'{host}'}/liv.sh</code></p>
            </div>
            <div>
              <label className="dm-label" style={{display: 'flex', alignItems: 'center', gap: 6}}>
                <span style={{width: 13, height: 13, color: 'var(--text-muted)', display: 'inline-flex'}}>{Icon.package}</span>
                Commande de génération
              </label>
              <input className="dm-input mono" placeholder="laisser vide pour masquer le bouton"/>
              <p className="dm-hint">Le bouton « Générer » ne s'affiche pas si ce champ est vide.</p>
            </div>
            <div>
              <label className="dm-label" style={{display: 'flex', alignItems: 'center', gap: 6}}>
                <span style={{width: 13, height: 13, color: 'var(--text-muted)', display: 'inline-flex'}}>{Icon.truck}</span>
                Commande de livraison
              </label>
              <input className="dm-input mono" placeholder="laisser vide pour masquer le bouton"/>
            </div>
            <div>
              <label className="dm-label">Timeout spécifique (minutes)</label>
              <div style={{display: 'flex', gap: 8, alignItems: 'center'}}>
                <input className="dm-input" type="number" style={{width: 100}} placeholder="10"/>
                <span className="dm-small dm-muted">Vide = utiliser le timeout global (10 min). <b>0</b> = désactivé.</span>
              </div>
            </div>
          </div>
        </div>

        <div style={{display: 'flex', justifyContent: 'flex-end', gap: 8}}>
          <button className="dm-btn">Annuler</button>
          <button className="dm-btn primary">Créer l'hôte</button>
        </div>
      </div>
    </AppShell>
  );
}

function AdminHostImport() {
  return (
    <AppShell active="hosts" role="admin"
              crumbs={['Hôtes', 'Import hosts-all']}>
      <div style={{maxWidth: 720}}>
        <div className="dm-card" style={{padding: 22, marginBottom: 18}}>
          <h3 style={{marginBottom: 4}}>Importer depuis un fichier Ansible</h3>
          <p className="dm-small dm-muted" style={{marginBottom: 16}}>
            Mettez à jour les hôtes existants à partir d'un inventaire <code className="dm-mono">hosts-all</code>. Les hôtes absents du fichier ne sont pas supprimés.
          </p>

          <div className="dm-dropzone">
            <div style={{color: 'var(--text-muted)', marginBottom: 8}}>
              <span style={{width: 32, height: 32, display: 'inline-flex'}}>{Icon.upload}</span>
            </div>
            <div style={{fontSize: 14, color: 'var(--text)', fontWeight: 500, marginBottom: 4}}>Déposez votre fichier ici</div>
            <div className="dm-small dm-muted" style={{marginBottom: 12}}>ou</div>
            <button className="dm-btn">Parcourir</button>
            <div className="dm-tiny dm-muted" style={{marginTop: 10}}>Formats acceptés : hosts, hosts-all, .ini</div>
          </div>

          <div style={{marginTop: 18, background: 'var(--accent-subtle)', border: '1px solid #d8dcff', borderRadius: 6, padding: 12, display: 'flex', gap: 10}}>
            <span style={{width: 16, height: 16, display: 'inline-flex', color: 'var(--accent-text)', flexShrink: 0}}>{Icon.info}</span>
            <div style={{fontSize: 12, color: 'var(--text)'}}>
              Champs extraits : <code className="dm-mono">hostname</code> → nom, <code className="dm-mono">ansible_host</code> → IP, <code className="dm-mono">domain_name</code> → domaine (optionnel).
              Les autres variables et groupes sont ignorés.
            </div>
          </div>
        </div>

        <div className="dm-card" style={{padding: 22}}>
          <h3 style={{marginBottom: 4}}>Aperçu</h3>
          <p className="dm-small dm-muted" style={{marginBottom: 14}}>3 hôtes seront mis à jour, 1 créé, 0 supprimé.</p>
          <table className="dm-table">
            <thead>
              <tr>
                <th>Hôte</th>
                <th>IP</th>
                <th>Domaine</th>
                <th style={{width: 120}}>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td className="mono">vpn</td>
                <td className="mono">10.42.1.12</td>
                <td className="mono">vpn.arthurbr02.fr</td>
                <td><span className="dm-badge neutral">Inchangé</span></td>
              </tr>
              <tr>
                <td className="mono">api-gateway</td>
                <td className="mono">10.42.1.30 <span className="dm-muted dm-tiny">(← 10.42.1.35)</span></td>
                <td className="mono">api.arthurbr02.fr</td>
                <td><span className="dm-badge progress"><span className="dot"/>Mis à jour</span></td>
              </tr>
              <tr>
                <td className="mono">redis</td>
                <td className="mono">10.42.1.21</td>
                <td className="mono">redis.arthurbr02.fr</td>
                <td><span className="dm-badge neutral">Inchangé</span></td>
              </tr>
              <tr>
                <td className="mono">new-worker</td>
                <td className="mono">10.42.1.70</td>
                <td className="mono">—</td>
                <td><span className="dm-badge success"><span className="dot"/>Créé</span></td>
              </tr>
            </tbody>
          </table>
          <div style={{display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 16}}>
            <button className="dm-btn">Annuler</button>
            <button className="dm-btn primary">Appliquer l'import</button>
          </div>
        </div>
      </div>
    </AppShell>
  );
}

function AdminSettings() {
  const [tab, setTab] = React.useState('deploy');
  return (
    <AppShell active="settings" role="admin" title="Paramètres">
      <div style={{display: 'grid', gridTemplateColumns: '200px 1fr', gap: 24, maxWidth: 900}}>
        <div style={{display: 'flex', flexDirection: 'column', gap: 2}}>
          {[
            { k: 'deploy', label: 'Déploiement', icon: Icon.rocket },
            { k: 'smtp', label: 'SMTP (email)', icon: Icon.mail },
            { k: 'security', label: 'Sécurité', icon: Icon.lock },
            { k: 'system', label: 'Système', icon: Icon.settings },
          ].map(it => (
            <div key={it.k}
                 onClick={() => setTab(it.k)}
                 style={{
                   padding: '8px 12px', borderRadius: 6, cursor: 'pointer',
                   display: 'flex', alignItems: 'center', gap: 10, fontSize: 13,
                   background: tab === it.k ? 'var(--bg-subtle)' : 'transparent',
                   color: tab === it.k ? 'var(--text)' : 'var(--text-muted)',
                   fontWeight: tab === it.k ? 500 : 400,
                 }}>
              <span style={{width: 14, height: 14, display: 'inline-flex'}}>{it.icon}</span>
              {it.label}
            </div>
          ))}
        </div>

        <div>
          {tab === 'deploy' && (
            <div className="dm-card" style={{padding: 22}}>
              <h3 style={{marginBottom: 4}}>Déploiement — valeurs par défaut</h3>
              <p className="dm-small dm-muted" style={{marginBottom: 18}}>
                Appliquées à tous les hôtes n'ayant pas de valeur personnalisée.
              </p>

              <div style={{marginBottom: 14}}>
                <label className="dm-label">Commande de déploiement par défaut</label>
                <input className="dm-input mono" defaultValue="sh /root/{host}/liv.sh"/>
                <p className="dm-hint">Variables : <code className="dm-mono">{'{host}'}</code>, <code className="dm-mono">{'{ip}'}</code>, <code className="dm-mono">{'{domain}'}</code>.</p>
              </div>

              <div style={{marginBottom: 14}}>
                <label className="dm-label">Timeout par défaut (minutes)</label>
                <div style={{display: 'flex', gap: 8}}>
                  <input className="dm-input" type="number" defaultValue="10" style={{width: 120}}/>
                  <span className="dm-small dm-muted" style={{alignSelf: 'center'}}>0 = désactivé</span>
                </div>
              </div>

              <div className="dm-hr"/>

              <div style={{display: 'flex', justifyContent: 'flex-end', gap: 8}}>
                <button className="dm-btn">Annuler</button>
                <button className="dm-btn primary">Enregistrer</button>
              </div>
            </div>
          )}

          {tab === 'smtp' && (
            <div className="dm-card" style={{padding: 22}}>
              <h3 style={{marginBottom: 4}}>Configuration SMTP</h3>
              <p className="dm-small dm-muted" style={{marginBottom: 18}}>Utilisée pour les emails de réinitialisation de mot de passe.</p>
              <div style={{display: 'grid', gridTemplateColumns: '1fr 120px', gap: 14}}>
                <div>
                  <label className="dm-label">Serveur SMTP</label>
                  <input className="dm-input mono" defaultValue="smtp.mailgun.org"/>
                </div>
                <div>
                  <label className="dm-label">Port</label>
                  <input className="dm-input mono" defaultValue="587"/>
                </div>
                <div>
                  <label className="dm-label">Utilisateur</label>
                  <input className="dm-input" defaultValue="postmaster@arthurbr02.fr"/>
                </div>
                <div>
                  <label className="dm-label">Mot de passe</label>
                  <input className="dm-input" type="password" defaultValue="••••••••"/>
                </div>
                <div style={{gridColumn: '1 / -1'}}>
                  <label className="dm-label">Adresse expéditeur (from)</label>
                  <input className="dm-input" defaultValue="noreply@arthurbr02.fr"/>
                </div>
              </div>
              <div className="dm-hr"/>
              <div style={{display: 'flex', justifyContent: 'space-between'}}>
                <button className="dm-btn">Envoyer un email de test</button>
                <div style={{display: 'flex', gap: 8}}>
                  <button className="dm-btn">Annuler</button>
                  <button className="dm-btn primary">Enregistrer</button>
                </div>
              </div>
            </div>
          )}

          {tab === 'security' && (
            <div className="dm-card" style={{padding: 22}}>
              <h3 style={{marginBottom: 4}}>Sécurité</h3>
              <p className="dm-small dm-muted" style={{marginBottom: 18}}>Durées de session et règles d'authentification.</p>
              <div style={{display: 'flex', flexDirection: 'column', gap: 16}}>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid var(--border-subtle)'}}>
                  <div>
                    <div style={{fontSize: 13, fontWeight: 500}}>Durée du access token</div>
                    <div className="dm-tiny dm-muted">Token JWT court (en mémoire)</div>
                  </div>
                  <input className="dm-input mono" defaultValue="15m" style={{width: 100}}/>
                </div>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid var(--border-subtle)'}}>
                  <div>
                    <div style={{fontSize: 13, fontWeight: 500}}>Durée du refresh token</div>
                    <div className="dm-tiny dm-muted">Cookie httpOnly long</div>
                  </div>
                  <input className="dm-input mono" defaultValue="7d" style={{width: 100}}/>
                </div>
                <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0'}}>
                  <div>
                    <div style={{fontSize: 13, fontWeight: 500}}>Durée du token de reset mot de passe</div>
                    <div className="dm-tiny dm-muted">Envoyé par email</div>
                  </div>
                  <input className="dm-input mono" defaultValue="1h" style={{width: 100}}/>
                </div>
              </div>
              <div className="dm-hr"/>
              <div style={{display: 'flex', justifyContent: 'flex-end', gap: 8}}>
                <button className="dm-btn primary">Enregistrer</button>
              </div>
            </div>
          )}

          {tab === 'system' && (
            <div className="dm-card" style={{padding: 22}}>
              <h3 style={{marginBottom: 4}}>Système</h3>
              <p className="dm-small dm-muted" style={{marginBottom: 18}}>Informations techniques (lecture seule).</p>
              <div style={{display: 'grid', gridTemplateColumns: 'auto 1fr', gap: '10px 20px', fontSize: 12.5}}>
                <span className="dm-muted">Version</span>                   <span className="dm-mono">v1.2.0-rc3</span>
                <span className="dm-muted">Base de données</span>           <span className="dm-mono">PostgreSQL 16.2</span>
                <span className="dm-muted">Répertoire des logs</span>       <span className="dm-mono">/var/log/deploy-manager</span>
                <span className="dm-muted">Espace disque logs</span>        <span className="dm-mono">2.4 GB / 20 GB</span>
                <span className="dm-muted">Déploiements actifs</span>       <span className="dm-mono">1</span>
                <span className="dm-muted">Uptime</span>                    <span className="dm-mono">14 j 7 h 22 min</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </AppShell>
  );
}

function ProfileScreen() {
  return (
    <AppShell active="profile" role="admin" title="Mon profil">
      <div style={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, maxWidth: 820}}>
        <div className="dm-card" style={{padding: 22}}>
          <h3 style={{marginBottom: 14}}>Informations</h3>
          <div style={{display: 'flex', alignItems: 'center', gap: 16, marginBottom: 18}}>
            <div className="dm-avatar" style={{width: 56, height: 56, fontSize: 18}}>AB</div>
            <div style={{display: 'flex', flexDirection: 'column', gap: 6}}>
              <button className="dm-btn sm">Changer l'avatar</button>
              <button className="dm-btn sm ghost" style={{color: 'var(--status-failure)'}}>Supprimer</button>
            </div>
          </div>
          <div style={{display: 'flex', flexDirection: 'column', gap: 12}}>
            <div>
              <label className="dm-label">Prénom</label>
              <input className="dm-input" defaultValue="Arthur"/>
            </div>
            <div>
              <label className="dm-label">Nom</label>
              <input className="dm-input" defaultValue="Br."/>
            </div>
            <div>
              <label className="dm-label">Email</label>
              <input className="dm-input" defaultValue="arthur@arthurbr02.fr" disabled/>
              <p className="dm-hint">Contactez un administrateur pour modifier votre email.</p>
            </div>
            <button className="dm-btn primary" style={{marginTop: 4, alignSelf: 'flex-start'}}>Enregistrer</button>
          </div>
        </div>

        <div className="dm-card" style={{padding: 22}}>
          <h3 style={{marginBottom: 14}}>Mot de passe</h3>
          <div style={{display: 'flex', flexDirection: 'column', gap: 12}}>
            <div>
              <label className="dm-label">Mot de passe actuel</label>
              <input className="dm-input" type="password"/>
            </div>
            <div>
              <label className="dm-label">Nouveau mot de passe</label>
              <input className="dm-input" type="password"/>
            </div>
            <div>
              <label className="dm-label">Confirmer</label>
              <input className="dm-input" type="password"/>
            </div>
            <button className="dm-btn primary" style={{marginTop: 4, alignSelf: 'flex-start'}}>Mettre à jour</button>
          </div>

          <div className="dm-hr"/>
          <h3 style={{marginBottom: 10}}>Session</h3>
          <button className="dm-btn">
            <span style={{width: 13, height: 13, display: 'inline-flex'}}>{Icon.logout}</span>
            Se déconnecter
          </button>
        </div>
      </div>
    </AppShell>
  );
}

function EmptyHostsScreen() {
  return (
    <AppShell active="hosts" role="user" title="Hôtes">
      <div style={{maxWidth: 520, margin: '80px auto 0', textAlign: 'center'}}>
        <div style={{width: 64, height: 64, borderRadius: 16, background: 'var(--bg-muted)', display: 'grid', placeItems: 'center', margin: '0 auto 18px', color: 'var(--text-subtle)'}}>
          <span style={{width: 28, height: 28, display: 'inline-flex'}}>{Icon.server}</span>
        </div>
        <h2 style={{fontSize: 17, marginBottom: 6}}>Aucun hôte accessible</h2>
        <p className="dm-small dm-muted" style={{marginBottom: 20}}>
          Un administrateur doit vous accorder l'accès à au moins un hôte pour commencer à déployer.
        </p>
        <button className="dm-btn">
          <span style={{width: 13, height: 13, display: 'inline-flex'}}>{Icon.mail}</span>
          Contacter un administrateur
        </button>
      </div>
    </AppShell>
  );
}

function NotFoundScreen() {
  return (
    <AppShell active="hosts" role="admin" title="Introuvable">
      <div style={{maxWidth: 420, margin: '80px auto 0', textAlign: 'center'}}>
        <div style={{fontFamily: 'var(--font-mono)', fontSize: 64, fontWeight: 600, color: 'var(--text-subtle)', letterSpacing: '-0.04em'}}>404</div>
        <h2 style={{fontSize: 17, marginTop: 8, marginBottom: 6}}>Page introuvable</h2>
        <p className="dm-small dm-muted" style={{marginBottom: 20}}>
          Cette ressource n'existe pas ou a été supprimée.
        </p>
        <button className="dm-btn primary">Retour aux hôtes</button>
      </div>
    </AppShell>
  );
}

function ErrorScreen() {
  return (
    <AppShell active="hosts" role="admin" title="Erreur">
      <div style={{maxWidth: 460, margin: '60px auto 0'}}>
        <div className="dm-card" style={{padding: 24, borderColor: 'var(--status-failure-border)', background: 'var(--status-failure-bg)'}}>
          <div style={{display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10, color: 'var(--status-failure)'}}>
            <span style={{width: 18, height: 18, display: 'inline-flex'}}>{Icon.alert}</span>
            <h3 style={{fontSize: 15, color: 'var(--status-failure)'}}>Déploiement impossible</h3>
          </div>
          <p className="dm-small" style={{color: 'var(--text)', marginBottom: 14}}>
            Un déploiement est déjà en cours sur cet hôte. Attendez sa fin ou annulez-le avant d'en lancer un nouveau.
          </p>
          <div className="dm-mono dm-tiny" style={{padding: 10, background: '#fff', border: '1px solid var(--status-failure-border)', borderRadius: 6, color: 'var(--text-muted)', marginBottom: 14}}>
            HTTP 409 · HOST_DEPLOYMENT_IN_PROGRESS
          </div>
          <div style={{display: 'flex', gap: 8}}>
            <button className="dm-btn">Voir le déploiement en cours</button>
            <button className="dm-btn ghost">Fermer</button>
          </div>
        </div>
      </div>
    </AppShell>
  );
}

function ToastStack() {
  return (
    <AppShell active="hosts" role="admin" title="Notifications">
      <p className="dm-small dm-muted" style={{marginBottom: 16}}>Exemples de toasts affichés lors des fins de déploiement.</p>
      <div style={{display: 'flex', flexDirection: 'column', gap: 10, maxWidth: 420}}>
        {[
          { status: 'SUCCESS',   title: 'Déploiement réussi', body: 'vpn · 2 min 14 s · lancé par vous' },
          { status: 'FAILURE',   title: 'Déploiement en échec', body: 'redis · code 1 · voir les logs' },
          { status: 'CANCELLED', title: 'Déploiement annulé', body: "worker-mail · par Arthur Br." },
          { status: 'IN_PROGRESS', title: 'Timeout dépassé',    body: 'api-gateway · arrêté après 15 min' },
        ].map((t, i) => {
          const color = {
            SUCCESS: 'var(--status-success)',
            FAILURE: 'var(--status-failure)',
            CANCELLED: 'var(--status-cancelled)',
            IN_PROGRESS: 'var(--status-progress)',
          }[t.status];
          const icon = t.status === 'SUCCESS' ? Icon.check : t.status === 'FAILURE' ? Icon.x : Icon.alert;
          return (
            <div key={i} className="dm-card" style={{padding: 12, display: 'flex', gap: 10, alignItems: 'flex-start', borderLeft: `3px solid ${color}`}}>
              <span style={{width: 18, height: 18, color, display: 'inline-flex', flexShrink: 0, marginTop: 1}}>{icon}</span>
              <div style={{flex: 1}}>
                <div style={{fontSize: 13, fontWeight: 500}}>{t.title}</div>
                <div className="dm-tiny dm-muted" style={{marginTop: 2}}>{t.body}</div>
              </div>
              <button className="dm-btn ghost sm" style={{padding: 3}}><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.x}</span></button>
            </div>
          );
        })}
      </div>
    </AppShell>
  );
}

Object.assign(window, {
  AdminUsers, AdminUserDetail, AdminUserCreated,
  AdminHostCreate, AdminHostImport, AdminSettings,
  ProfileScreen, EmptyHostsScreen, NotFoundScreen, ErrorScreen, ToastStack, SAMPLE_USERS
});
