// Hosts list — /hosts — 2 variants
// V1: Liste dense (tableau-like rows) avec actions inline
// V2: Cartes compactes en grille

const SAMPLE_HOSTS = [
  { id: '1',  name: 'vpn',           ip: '10.42.1.12',   domain: 'vpn.arthurbr02.fr',          lastStatus: 'SUCCESS',     lastAt: 'il y a 12 min',   lastUser: 'Arthur Br.',   can: ['deploy','generate','deliver'] },
  { id: '2',  name: 'postgres-prod', ip: '10.42.1.20',   domain: 'db.arthurbr02.fr',           lastStatus: 'IN_PROGRESS', lastAt: '2 min 14s',        lastUser: 'Sophie M.',    can: ['deploy'] },
  { id: '3',  name: 'redis',         ip: '10.42.1.21',   domain: 'redis.arthurbr02.fr',        lastStatus: 'FAILURE',     lastAt: 'il y a 1 h',       lastUser: 'Arthur Br.',   can: ['deploy','generate'] },
  { id: '4',  name: 'api-gateway',   ip: '10.42.1.30',   domain: 'api.arthurbr02.fr',          lastStatus: 'SUCCESS',     lastAt: 'il y a 3 h',       lastUser: 'Julien L.',    can: ['deploy','generate','deliver'] },
  { id: '5',  name: 'frontend',      ip: '10.42.1.31',   domain: 'app.arthurbr02.fr',          lastStatus: 'SUCCESS',     lastAt: 'il y a 5 h',       lastUser: 'Sophie M.',    can: ['deploy','deliver'] },
  { id: '6',  name: 'worker-mail',   ip: '10.42.1.40',   domain: null,                          lastStatus: 'CANCELLED',   lastAt: 'hier, 14:02',     lastUser: 'Arthur Br.',   can: ['deploy'] },
  { id: '7',  name: 'monitoring',    ip: '10.42.1.50',   domain: 'grafana.arthurbr02.fr',      lastStatus: 'SUCCESS',     lastAt: 'il y a 2 j',       lastUser: 'Julien L.',    can: ['deploy','generate','deliver'] },
  { id: '8',  name: 'minio',         ip: '10.42.1.60',   domain: 's3.arthurbr02.fr',           lastStatus: 'NEVER',       lastAt: null,               lastUser: null,           can: ['deploy'] },
  { id: '9',  name: 'nginx-edge',    ip: '10.42.1.1',    domain: 'arthurbr02.fr',              lastStatus: 'SUCCESS',     lastAt: 'il y a 2 j',       lastUser: 'Arthur Br.',   can: ['deploy','generate','deliver'] },
];

function HostActions({ host, mode = 'split' }) {
  const has = (k) => host.can.includes(k);
  if (mode === 'menu') {
    return (
      <div style={{display: 'flex', gap: 4}}>
        <button className="dm-btn primary sm" disabled={host.lastStatus === 'IN_PROGRESS'}>
          <span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.rocket}</span>
          Déployer
        </button>
        <button className="dm-btn sm" style={{padding: '5px 6px'}}>{Icon.chevron}</button>
      </div>
    );
  }
  if (mode === 'stack') {
    return (
      <div style={{display: 'flex', flexDirection: 'column', gap: 4}}>
        {has('deploy') && <button className="dm-btn primary sm" style={{justifyContent: 'flex-start'}}><span style={{width: 11, height: 11, display: 'inline-flex'}}>{Icon.rocket}</span>Déployer</button>}
        {has('generate') && <button className="dm-btn sm" style={{justifyContent: 'flex-start'}}><span style={{width: 11, height: 11, display: 'inline-flex'}}>{Icon.package}</span>Générer</button>}
        {has('deliver') && <button className="dm-btn sm" style={{justifyContent: 'flex-start'}}><span style={{width: 11, height: 11, display: 'inline-flex'}}>{Icon.truck}</span>Livrer</button>}
      </div>
    );
  }
  // split: 3 boutons côte à côte (deploy primary)
  return (
    <div style={{display: 'flex', gap: 4, flexWrap: 'wrap'}}>
      {has('deploy') && (
        <button className="dm-btn primary sm" disabled={host.lastStatus === 'IN_PROGRESS'}
                title={`sh /root/${host.name}/liv.sh`}>
          <span style={{width: 11, height: 11, display: 'inline-flex'}}>{Icon.rocket}</span>
          Déployer
        </button>
      )}
      {has('generate') && (
        <button className="dm-btn sm" title={`sh /root/${host.name}/gen.sh`}>
          <span style={{width: 11, height: 11, display: 'inline-flex'}}>{Icon.package}</span>
          Générer
        </button>
      )}
      {has('deliver') && (
        <button className="dm-btn sm" title={`sh /root/${host.name}/deliver.sh`}>
          <span style={{width: 11, height: 11, display: 'inline-flex'}}>{Icon.truck}</span>
          Livrer
        </button>
      )}
    </div>
  );
}

function HostsListV1() {
  // Rows denses — grosse vue admin, très actionnable
  return (
    <AppShell active="hosts" role="admin"
              title="Hôtes"
              topRight={<>
                <div style={{position: 'relative'}}>
                  <span style={{position: 'absolute', left: 10, top: 8, width: 14, height: 14, color: 'var(--text-subtle)'}}>{Icon.search}</span>
                  <input className="dm-input" placeholder="Rechercher un hôte…" style={{paddingLeft: 30, width: 240}}/>
                </div>
                <button className="dm-btn"><span style={{width: 14, height: 14, display: 'inline-flex'}}>{Icon.filter}</span>Filtrer</button>
                <button className="dm-btn"><span style={{width: 14, height: 14, display: 'inline-flex'}}>{Icon.upload}</span>Import Ansible</button>
                <button className="dm-btn primary">{Icon.plus}Nouvel hôte</button>
              </>}>
      <div style={{display: 'flex', gap: 10, marginBottom: 16, fontSize: 12, alignItems: 'center'}}>
        <span className="dm-muted">24 hôtes ·</span>
        <span className="dm-badge success"><span className="dot"/>22 stables</span>
        <span className="dm-badge progress"><span className="dm-spin" style={{width: 9, height: 9, borderWidth: 1.5}}/>1 en cours</span>
        <span className="dm-badge failure"><span className="dot"/>1 en échec</span>
      </div>
      <div className="dm-card" style={{overflow: 'hidden'}}>
        <table className="dm-table">
          <thead>
            <tr>
              <th style={{width: '22%'}}>Hôte</th>
              <th style={{width: '14%'}}>IP</th>
              <th style={{width: '22%'}}>Domaine</th>
              <th style={{width: '18%'}}>Dernier déploiement</th>
              <th style={{width: '24%', textAlign: 'right'}}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {SAMPLE_HOSTS.map(h => (
              <tr key={h.id}>
                <td>
                  <div style={{display: 'flex', alignItems: 'center', gap: 10}}>
                    <span style={{width: 16, height: 16, color: 'var(--text-subtle)', display: 'inline-flex'}}>{Icon.server}</span>
                    <div>
                      <div style={{fontWeight: 500}}>{h.name}</div>
                    </div>
                  </div>
                </td>
                <td className="mono">{h.ip}</td>
                <td className="mono">{h.domain || <span className="dm-subtle">—</span>}</td>
                <td>
                  <div style={{display: 'flex', flexDirection: 'column', gap: 3}}>
                    <StatusBadge status={h.lastStatus}/>
                    {h.lastAt && <span className="dm-tiny dm-muted">{h.lastAt}{h.lastUser ? ` · ${h.lastUser}` : ''}</span>}
                  </div>
                </td>
                <td style={{textAlign: 'right'}}>
                  <div style={{display: 'flex', justifyContent: 'flex-end'}}>
                    <HostActions host={h} mode="split"/>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div style={{marginTop: 14, fontSize: 11.5, color: 'var(--text-muted)', display: 'flex', justifyContent: 'space-between'}}>
        <span>9 hôtes visibles sur 24</span>
        <div style={{display: 'flex', gap: 4}}>
          <button className="dm-btn sm ghost">← Précédent</button>
          <button className="dm-btn sm ghost">Suivant →</button>
        </div>
      </div>
    </AppShell>
  );
}

function HostsListV2({ dark }) {
  // Cartes compactes, grille 3 colonnes
  return (
    <AppShell active="hosts" role="admin" dark={dark}
              title="Hôtes"
              topRight={<>
                <div style={{position: 'relative'}}>
                  <span style={{position: 'absolute', left: 10, top: 8, width: 14, height: 14, color: 'var(--text-subtle)'}}>{Icon.search}</span>
                  <input className="dm-input" placeholder="Rechercher un hôte, une IP, un domaine…" style={{paddingLeft: 30, width: 280}}/>
                </div>
                <button className="dm-btn"><span style={{width: 14, height: 14, display: 'inline-flex'}}>{Icon.upload}</span>Import</button>
                <button className="dm-btn primary">{Icon.plus}Nouvel hôte</button>
              </>}>

      {/* Status summary strip */}
      <div style={{display: 'flex', gap: 10, marginBottom: 14, fontSize: 12, alignItems: 'center'}}>
        <span className="dm-muted">24 hôtes ·</span>
        <span className="dm-badge success"><span className="dot"/>22 stables</span>
        <span className="dm-badge progress"><span className="dm-spin" style={{width: 9, height: 9, borderWidth: 1.5}}/>1 en cours</span>
        <span className="dm-badge failure"><span className="dot"/>1 en échec</span>
      </div>

      <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16}}>
        <div style={{display: 'flex', gap: 4}}>
          {['Tous (24)', 'Stables (22)', 'En cours (1)', 'En échec (1)', 'Favoris'].map((t, i) => (
            <button key={i} className={`dm-btn sm ${i === 0 ? '' : 'ghost'}`} style={i === 0 ? {background: 'var(--bg-subtle)'} : {}}>{t}</button>
          ))}
        </div>
        <div className="dm-small dm-muted">Trier : <b style={{color: 'var(--text)'}}>Activité récente ▾</b></div>
      </div>

      <div style={{display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14}}>
        {SAMPLE_HOSTS.map(h => (
          <div key={h.id} className="dm-card" style={{padding: 14, display: 'flex', flexDirection: 'column', gap: 12}}>
            <div style={{display: 'flex', alignItems: 'flex-start', gap: 10}}>
              <div style={{width: 32, height: 32, borderRadius: 8, background: 'var(--bg-muted)', display: 'grid', placeItems: 'center', flexShrink: 0, color: 'var(--text-muted)'}}>
                <span style={{width: 16, height: 16, display: 'inline-flex'}}>{Icon.server}</span>
              </div>
              <div style={{flex: 1, minWidth: 0}}>
                <div style={{display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2}}>
                  <span style={{fontWeight: 600, fontSize: 13.5}}>{h.name}</span>
                  <StatusBadge status={h.lastStatus}/>
                </div>
                <div className="dm-mono dm-tiny dm-muted" style={{display: 'flex', gap: 10, flexWrap: 'wrap'}}>
                  <span>{h.ip}</span>
                  {h.domain && <span style={{color: 'var(--text-subtle)'}}>·</span>}
                  {h.domain && <span>{h.domain}</span>}
                </div>
              </div>
              <button className="dm-btn ghost sm" style={{padding: '3px 5px'}}>{Icon.more}</button>
            </div>

            <div className="dm-tiny dm-muted" style={{borderTop: '1px solid var(--border-subtle)', paddingTop: 8, display: 'flex', justifyContent: 'space-between'}}>
              <span>{h.lastAt ? `${h.lastAt}` : 'Jamais déployé'}</span>
              {h.lastUser && <span>{h.lastUser}</span>}
            </div>

            <HostActions host={h} mode="split"/>
          </div>
        ))}
      </div>
    </AppShell>
  );
}

Object.assign(window, { HostsListV1, HostsListV2, SAMPLE_HOSTS, HostActions });
