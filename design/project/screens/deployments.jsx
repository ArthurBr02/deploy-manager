// Deployments history /deployments + deployment detail viewer

function DeploymentsHistory() {
  const all = [
    ...SAMPLE_DEPLOYMENTS.map(d => ({...d, host: 'vpn'})),
    { id: 'b4e2f1', type: 'DEPLOY',   user: 'Sophie M.',  status: 'SUCCESS',     duration: '03:22', at: 'lun. 10:12', host: 'postgres-prod' },
    { id: '9e7a23', type: 'GENERATE', user: 'Arthur Br.', status: 'SUCCESS',     duration: '00:52', at: 'ven. 17:40', host: 'api-gateway' },
    { id: '1c8d4b', type: 'DELIVER',  user: 'Julien L.',  status: 'FAILURE',     duration: '00:18', at: 'ven. 16:02', host: 'frontend' },
    { id: '5a2e90', type: 'DEPLOY',   user: 'Sophie M.',  status: 'SUCCESS',     duration: '02:04', at: 'jeu. 11:30', host: 'redis' },
    { id: '8f3d10', type: 'DEPLOY',   user: 'Arthur Br.', status: 'CANCELLED',   duration: '00:08', at: 'jeu. 09:00', host: 'worker-mail' },
  ];
  return (
    <AppShell active="deployments" role="admin"
              title="Déploiements"
              topRight={<button className="dm-btn"><span style={{width: 14, height: 14, display: 'inline-flex'}}>{Icon.download}</span>Exporter CSV</button>}>
      <p className="dm-small dm-muted" style={{marginBottom: 16}}>
        Historique global — tous les hôtes, tous les utilisateurs.
      </p>

      {/* Filters row */}
      <div className="dm-card" style={{padding: 12, marginBottom: 14, display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center'}}>
        <div style={{position: 'relative', minWidth: 240, flex: 1}}>
          <span style={{position: 'absolute', left: 10, top: 8, width: 14, height: 14, color: 'var(--text-subtle)'}}>{Icon.search}</span>
          <input className="dm-input" placeholder="Rechercher (ID, hôte, utilisateur…)" style={{paddingLeft: 30}}/>
        </div>
        <select className="dm-select" style={{width: 160}} defaultValue="all-hosts">
          <option value="all-hosts">Tous les hôtes</option>
          <option>vpn</option>
          <option>postgres-prod</option>
        </select>
        <select className="dm-select" style={{width: 140}} defaultValue="all-status">
          <option value="all-status">Tous les statuts</option>
          <option>Succès</option>
          <option>Échec</option>
          <option>En cours</option>
          <option>Annulé</option>
        </select>
        <select className="dm-select" style={{width: 140}} defaultValue="all-types">
          <option value="all-types">Tous les types</option>
          <option>Déployer</option>
          <option>Générer</option>
          <option>Livrer</option>
        </select>
        <select className="dm-select" style={{width: 150}} defaultValue="7d">
          <option value="24h">Dernières 24 h</option>
          <option value="7d">7 derniers jours</option>
          <option value="30d">30 derniers jours</option>
          <option value="all">Depuis toujours</option>
        </select>
        <button className="dm-btn ghost sm">Réinitialiser</button>
      </div>

      {/* Stats strip */}
      <div style={{display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 14}}>
        {[
          { label: 'Total', v: '284', sub: '7 derniers jours' },
          { label: 'Réussis', v: '261', sub: '92 % de succès', color: 'var(--status-success)' },
          { label: 'En échec', v: '14',  sub: '5 %', color: 'var(--status-failure)' },
          { label: 'Durée médiane', v: '1 min 48 s', sub: '−12 % vs semaine passée' },
        ].map((s, i) => (
          <div key={i} className="dm-card dm-card-pad">
            <div className="dm-tiny dm-muted" style={{textTransform: 'uppercase', letterSpacing: '.05em'}}>{s.label}</div>
            <div style={{fontSize: 22, fontWeight: 600, marginTop: 4, color: s.color || 'var(--text)', letterSpacing: '-0.02em'}}>{s.v}</div>
            <div className="dm-tiny dm-muted" style={{marginTop: 2}}>{s.sub}</div>
          </div>
        ))}
      </div>

      <div className="dm-card" style={{overflow: 'hidden'}}>
        <table className="dm-table">
          <thead>
            <tr>
              <th style={{width: 100}}>ID</th>
              <th>Hôte</th>
              <th>Type</th>
              <th>Statut</th>
              <th>Lancé par</th>
              <th>Durée</th>
              <th>Date</th>
              <th style={{width: 40}}/>
            </tr>
          </thead>
          <tbody>
            {all.map(d => (
              <tr key={d.id}>
                <td className="mono">#{d.id}</td>
                <td><span style={{fontWeight: 500}}>{d.host}</span></td>
                <td><TypeBadge type={d.type}/></td>
                <td><StatusBadge status={d.status}/></td>
                <td>{d.user}</td>
                <td className="mono">{d.duration}</td>
                <td className="dm-muted">{d.at}</td>
                <td><button className="dm-btn ghost sm" style={{padding: 4}}><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.eye}</span></button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{marginTop: 14, fontSize: 11.5, color: 'var(--text-muted)', display: 'flex', justifyContent: 'space-between', alignItems: 'center'}}>
        <span>1 – 20 sur 284 déploiements</span>
        <div style={{display: 'flex', gap: 4, alignItems: 'center'}}>
          <button className="dm-btn sm ghost">← Précédent</button>
          <span className="dm-small" style={{padding: '0 8px'}}>Page <b>1</b> / 15</span>
          <button className="dm-btn sm">Suivant →</button>
        </div>
      </div>
    </AppShell>
  );
}

Object.assign(window, { DeploymentsHistory });
