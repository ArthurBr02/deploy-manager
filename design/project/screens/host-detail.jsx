// Host detail /hosts/:id — with 3 log view variants + deployment run

function LogsTerminal({ compact }) {
  return (
    <div style={{display: 'flex', flexDirection: 'column', height: compact ? 340 : 480, minHeight: 0}}>
      <div className="dm-term-head">
        <div className="dots"><span/><span/><span/></div>
        <span>vpn · deployment #a8f2e1 · sh /root/vpn/liv.sh</span>
        <div style={{marginLeft: 'auto', display: 'flex', gap: 8, alignItems: 'center'}}>
          <span className="dm-badge progress" style={{background: 'transparent', borderColor: '#3a3832', color: 'var(--term-amber)'}}>
            <span className="dm-spin" style={{width: 9, height: 9, borderWidth: 1.5, borderTopColor: 'var(--term-amber)'}}/>
            En cours · 01:42
          </span>
          <button className="dm-btn sm" style={{background: '#2a2927', color: '#e7e5df', borderColor: '#3a3832'}}>
            <span style={{width: 11, height: 11, display: 'inline-flex'}}>{Icon.stop}</span>Annuler
          </button>
        </div>
      </div>
      <pre className="dm-term dm-scroll" style={{margin: 0, flex: 1, borderTopLeftRadius: 0, borderTopRightRadius: 0, overflow: 'auto'}}>
<span className="m">[14:22:03] </span><span className="b">→</span> Connexion à l'hôte <span className="a">vpn</span> (10.42.1.12)...{'\n'}
<span className="m">[14:22:03] </span><span className="g">✓</span> SSH connection established{'\n'}
<span className="m">[14:22:04] </span><span className="b">$</span> cd /root/vpn && git fetch --all{'\n'}
<span className="m">[14:22:05] </span>From github.com:arthurbr02/vpn{'\n'}
<span className="m">[14:22:05] </span>   a3f2e91..b8d4c12  main       → origin/main{'\n'}
<span className="m">[14:22:05] </span><span className="b">$</span> git reset --hard origin/main{'\n'}
<span className="m">[14:22:06] </span>HEAD is now at b8d4c12 fix: retry logic on gateway timeout{'\n'}
<span className="m">[14:22:06] </span><span className="b">$</span> docker compose pull{'\n'}
<span className="m">[14:22:07] </span>[+] Pulling 3/3{'\n'}
<span className="m">[14:22:12] </span> ✔ vpn-server        Pulled        4.8s{'\n'}
<span className="m">[14:22:15] </span> ✔ vpn-monitoring    Pulled        7.2s{'\n'}
<span className="m">[14:22:19] </span> ✔ vpn-db            Pulled        11.1s{'\n'}
<span className="m">[14:22:19] </span><span className="b">$</span> docker compose up -d --remove-orphans{'\n'}
<span className="m">[14:22:20] </span>[+] Running 3/3{'\n'}
<span className="m">[14:22:22] </span> ✔ Container vpn-db           Started{'\n'}
<span className="m">[14:22:24] </span> ✔ Container vpn-server       Started{'\n'}
<span className="m">[14:22:26] </span> ✔ Container vpn-monitoring   Started{'\n'}
<span className="m">[14:22:26] </span><span className="b">$</span> ./scripts/healthcheck.sh{'\n'}
<span className="m">[14:22:27] </span>Waiting for services to become healthy...{'\n'}
<span className="m">[14:22:32] </span><span className="g">✓</span> vpn-db        healthy{'\n'}
<span className="m">[14:22:38] </span><span className="g">✓</span> vpn-server    healthy{'\n'}
<span className="m">[14:22:42] </span><span className="a">⋯</span> vpn-monitoring   booting (prometheus scrape){'\n'}
<span style={{background: '#2a2927', color: '#fff', padding: '0 2px'}}>▊</span>
      </pre>
    </div>
  );
}

function HostInfo({ host }) {
  return (
    <div className="dm-card dm-card-pad" style={{display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16}}>
      <div>
        <div className="dm-tiny dm-muted" style={{textTransform: 'uppercase', letterSpacing: '.05em', marginBottom: 4}}>IP</div>
        <div className="dm-mono dm-small">{host.ip}</div>
      </div>
      <div>
        <div className="dm-tiny dm-muted" style={{textTransform: 'uppercase', letterSpacing: '.05em', marginBottom: 4}}>Domaine</div>
        <div className="dm-mono dm-small">{host.domain}</div>
      </div>
      <div>
        <div className="dm-tiny dm-muted" style={{textTransform: 'uppercase', letterSpacing: '.05em', marginBottom: 4}}>Timeout</div>
        <div className="dm-small">15 min <span className="dm-muted dm-tiny">(par défaut : 10 min)</span></div>
      </div>
      <div>
        <div className="dm-tiny dm-muted" style={{textTransform: 'uppercase', letterSpacing: '.05em', marginBottom: 4}}>Dernier déploiement</div>
        <div><StatusBadge status="SUCCESS"/> <span className="dm-tiny dm-muted">il y a 3 h</span></div>
      </div>
    </div>
  );
}

function CommandsCard({ host }) {
  const cmds = [
    { label: 'Déployer', cmd: `sh /root/${host.name}/liv.sh`, icon: Icon.rocket },
    { label: 'Générer',  cmd: `sh /root/${host.name}/gen.sh`, icon: Icon.package },
    { label: 'Livrer',   cmd: `sh /root/${host.name}/deliver.sh`, icon: Icon.truck },
  ];
  return (
    <div className="dm-card">
      <div className="dm-card-head">
        <h3>Commandes</h3>
        <button className="dm-btn sm ghost"><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.edit}</span>Modifier</button>
      </div>
      <div style={{padding: 4}}>
        {cmds.map((c, i) => (
          <div key={i} style={{padding: '10px 12px', display: 'flex', alignItems: 'center', gap: 12, borderBottom: i < 2 ? '1px solid var(--border-subtle)' : 'none'}}>
            <span style={{width: 14, height: 14, color: 'var(--text-muted)', display: 'inline-flex'}}>{c.icon}</span>
            <span style={{fontSize: 12.5, fontWeight: 500, width: 70}}>{c.label}</span>
            <code style={{flex: 1, fontSize: 11.5, color: 'var(--text-muted)', fontFamily: 'var(--font-mono)'}}>{c.cmd}</code>
            <button className="dm-btn sm ghost" style={{padding: 4}}><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.copy}</span></button>
          </div>
        ))}
      </div>
    </div>
  );
}

function DeployButtons({ host, size = 'lg' }) {
  return (
    <div style={{display: 'flex', gap: 8}}>
      <button className={`dm-btn primary ${size}`} title={`sh /root/${host.name}/liv.sh`}>
        <span style={{width: 14, height: 14, display: 'inline-flex'}}>{Icon.rocket}</span>
        Déployer
      </button>
      <button className={`dm-btn ${size}`} title={`sh /root/${host.name}/gen.sh`}>
        <span style={{width: 14, height: 14, display: 'inline-flex'}}>{Icon.package}</span>
        Générer
      </button>
      <button className={`dm-btn ${size}`} title={`sh /root/${host.name}/deliver.sh`}>
        <span style={{width: 14, height: 14, display: 'inline-flex'}}>{Icon.truck}</span>
        Livrer
      </button>
    </div>
  );
}

const SAMPLE_DEPLOYMENTS = [
  { id: 'a8f2e1', type: 'DEPLOY',   user: 'Arthur Br.', status: 'IN_PROGRESS', duration: '01:42', at: "aujourd'hui, 14:22" },
  { id: 'f21a4c', type: 'DEPLOY',   user: 'Sophie M.',  status: 'SUCCESS',     duration: '02:14', at: "aujourd'hui, 11:08" },
  { id: 'c9b3d5', type: 'GENERATE', user: 'Arthur Br.', status: 'SUCCESS',     duration: '00:48', at: 'hier, 18:30' },
  { id: '7a21b8', type: 'DELIVER',  user: 'Julien L.',  status: 'FAILURE',     duration: '00:32', at: 'hier, 16:12' },
  { id: '4e9c11', type: 'DEPLOY',   user: 'Arthur Br.', status: 'CANCELLED',   duration: '00:15', at: 'hier, 10:40' },
  { id: '2d7f8a', type: 'DEPLOY',   user: 'Sophie M.',  status: 'SUCCESS',     duration: '01:52', at: 'lun. 09:14' },
];

function HistoryTable({ compact }) {
  const rows = compact ? SAMPLE_DEPLOYMENTS.slice(0, 5) : SAMPLE_DEPLOYMENTS;
  return (
    <div className="dm-card" style={{overflow: 'hidden'}}>
      <table className="dm-table">
        <thead>
          <tr>
            <th style={{width: 110}}>ID</th>
            <th>Type</th>
            <th>Lancé par</th>
            <th>Statut</th>
            <th>Durée</th>
            <th>Date</th>
            <th style={{width: 40}}/>
          </tr>
        </thead>
        <tbody>
          {rows.map(d => (
            <tr key={d.id}>
              <td className="mono">#{d.id}</td>
              <td><TypeBadge type={d.type}/></td>
              <td>{d.user}</td>
              <td><StatusBadge status={d.status}/></td>
              <td className="mono">{d.duration}</td>
              <td className="dm-muted">{d.at}</td>
              <td><button className="dm-btn ghost sm" style={{padding: 4}}><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.eye}</span></button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// VARIANT A — inline logs (under host info)
function HostDetailInline() {
  const host = SAMPLE_HOSTS[0];
  const [tab, setTab] = React.useState('overview');
  return (
    <AppShell active="hosts" role="admin"
              crumbs={['Hôtes', host.name]}
              topRight={<DeployButtons host={host} size=""/>}>
      <div style={{display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16}}>
        <div style={{width: 40, height: 40, borderRadius: 10, background: 'var(--bg-muted)', display: 'grid', placeItems: 'center', color: 'var(--text-muted)'}}>
          <span style={{width: 20, height: 20, display: 'inline-flex'}}>{Icon.server}</span>
        </div>
        <div>
          <h2 style={{fontSize: 18, marginBottom: 2}}>{host.name}</h2>
          <div className="dm-small dm-muted dm-mono">{host.ip} · {host.domain}</div>
        </div>
        <div style={{marginLeft: 'auto'}}>
          <StatusBadge status="IN_PROGRESS"/>
        </div>
      </div>

      <div className="dm-tabs">
        {['overview', 'history', 'settings', 'permissions'].map(t => (
          <div key={t} className={`dm-tab ${t === tab ? 'active' : ''}`} onClick={() => setTab(t)}>
            {({overview: 'Vue d\'ensemble', history: 'Historique (142)', settings: 'Paramètres', permissions: 'Accès (4)'})[t]}
          </div>
        ))}
      </div>

      <div style={{display: 'flex', flexDirection: 'column', gap: 14}}>
        <HostInfo host={host}/>

        <div className="dm-card" style={{overflow: 'hidden'}}>
          <div style={{padding: '12px 16px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 10}}>
            <span style={{width: 14, height: 14, color: 'var(--text-muted)', display: 'inline-flex'}}>{Icon.terminal}</span>
            <h3 style={{fontSize: 13}}>Déploiement en cours</h3>
            <span className="dm-tiny dm-muted">·</span>
            <span className="dm-tiny dm-mono dm-muted">#a8f2e1 · lancé par Arthur Br.</span>
            <div style={{marginLeft: 'auto', display: 'flex', gap: 6}}>
              <button className="dm-btn sm ghost">Dérouler auto</button>
              <button className="dm-btn sm ghost"><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.download}</span>Télécharger</button>
            </div>
          </div>
          <LogsTerminal compact/>
        </div>

        <CommandsCard host={host}/>

        <div>
          <div style={{display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8}}>
            <h3 style={{fontSize: 13}}>Historique récent</h3>
            <a className="dm-small" style={{color: 'var(--accent)', textDecoration: 'none'}}>Voir tout →</a>
          </div>
          <HistoryTable compact/>
        </div>
      </div>
    </AppShell>
  );
}

// VARIANT B — terminal prend tout l'écran principal (drawer style from right)
function HostDetailDrawer() {
  const host = SAMPLE_HOSTS[0];
  return (
    <div className={`dm ${window.__dmDark?'dark':''}`}>
      <Sidebar active="hosts" role="admin"/>
      <div className="dm-main">
        <Topbar crumbs={['Hôtes', host.name]} right={<DeployButtons host={host} size=""/>}/>
        <div style={{flex: 1, display: 'flex', minHeight: 0}}>
          {/* main area */}
          <div className="dm-content dm-scroll" style={{flex: 1, paddingRight: 24}}>
            <div style={{display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16}}>
              <div style={{width: 40, height: 40, borderRadius: 10, background: 'var(--bg-muted)', display: 'grid', placeItems: 'center', color: 'var(--text-muted)'}}>
                <span style={{width: 20, height: 20, display: 'inline-flex'}}>{Icon.server}</span>
              </div>
              <div>
                <h2 style={{fontSize: 18, marginBottom: 2}}>{host.name}</h2>
                <div className="dm-small dm-muted dm-mono">{host.ip} · {host.domain}</div>
              </div>
            </div>
            <HostInfo host={host}/>
            <div style={{height: 14}}/>
            <CommandsCard host={host}/>
            <div style={{height: 14}}/>
            <h3 style={{fontSize: 13, marginBottom: 8}}>Historique</h3>
            <HistoryTable compact/>
          </div>
          {/* side drawer with logs */}
          <div style={{width: 520, borderLeft: '1px solid var(--border)', background: 'var(--bg-panel)', display: 'flex', flexDirection: 'column'}}>
            <div style={{padding: '12px 16px', borderBottom: '1px solid var(--border)', display: 'flex', alignItems: 'center', gap: 10}}>
              <span style={{width: 14, height: 14, color: 'var(--text-muted)', display: 'inline-flex'}}>{Icon.terminal}</span>
              <div>
                <div style={{fontSize: 13, fontWeight: 600}}>Déploiement en cours</div>
                <div className="dm-tiny dm-muted dm-mono">#a8f2e1</div>
              </div>
              <div style={{marginLeft: 'auto'}}>
                <button className="dm-btn ghost sm" style={{padding: 4}}><span style={{width: 14, height: 14, display: 'inline-flex'}}>{Icon.x}</span></button>
              </div>
            </div>
            <div style={{flex: 1, padding: 12, minHeight: 0, display: 'flex', flexDirection: 'column'}}>
              <div style={{flex: 1, minHeight: 0}}>
                <LogsTerminal/>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// VARIANT C — modal full-screen terminal on launch
function HostDetailModal() {
  const host = SAMPLE_HOSTS[0];
  return (
    <div className={`dm ${window.__dmDark?'dark':''}`} style={{position: 'relative'}}>
      <Sidebar active="hosts" role="admin"/>
      <div className="dm-main">
        <Topbar crumbs={['Hôtes', host.name]} right={<DeployButtons host={host} size=""/>}/>
        <div className="dm-content dm-scroll">
          <div style={{display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16}}>
            <div style={{width: 40, height: 40, borderRadius: 10, background: 'var(--bg-muted)', display: 'grid', placeItems: 'center', color: 'var(--text-muted)'}}>
              <span style={{width: 20, height: 20, display: 'inline-flex'}}>{Icon.server}</span>
            </div>
            <div>
              <h2 style={{fontSize: 18, marginBottom: 2}}>{host.name}</h2>
              <div className="dm-small dm-muted dm-mono">{host.ip} · {host.domain}</div>
            </div>
          </div>
          <HostInfo host={host}/>
          <div style={{height: 14}}/>
          <CommandsCard host={host}/>
        </div>
      </div>

      {/* Modal overlay */}
      <div className="dm-overlay" style={{background: 'rgba(20,18,14,.65)'}}>
        <div style={{width: 780, maxWidth: '90%', background: 'var(--bg-panel)', borderRadius: 12, boxShadow: 'var(--sh-pop)', overflow: 'hidden', display: 'flex', flexDirection: 'column', maxHeight: '85%'}}>
          <div style={{padding: '14px 18px', display: 'flex', alignItems: 'center', gap: 10, borderBottom: '1px solid var(--border)'}}>
            <span style={{width: 14, height: 14, color: 'var(--text-muted)', display: 'inline-flex'}}>{Icon.terminal}</span>
            <div style={{flex: 1}}>
              <div style={{fontSize: 14, fontWeight: 600}}>Déploiement de {host.name}</div>
              <div className="dm-tiny dm-muted dm-mono">#a8f2e1 · Arthur Br. · démarré il y a 1 min 42 s</div>
            </div>
            <StatusBadge status="IN_PROGRESS"/>
            <button className="dm-btn ghost sm" style={{padding: 6}}><span style={{width: 14, height: 14, display: 'inline-flex'}}>{Icon.x}</span></button>
          </div>
          <div style={{flex: 1, padding: 14, minHeight: 0, display: 'flex', flexDirection: 'column'}}>
            <LogsTerminal/>
          </div>
          <div style={{padding: '10px 16px', borderTop: '1px solid var(--border)', background: 'var(--bg-subtle)', display: 'flex', alignItems: 'center', gap: 10}}>
            <span className="dm-tiny dm-muted">Les logs sont persistés en base à la fin du déploiement.</span>
            <div style={{marginLeft: 'auto', display: 'flex', gap: 6}}>
              <button className="dm-btn sm"><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.download}</span>Télécharger</button>
              <button className="dm-btn danger sm"><span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.stop}</span>Annuler</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Modal de lancement (timeout)
function DeployLaunchModal() {
  const host = SAMPLE_HOSTS[0];
  return (
    <div className={`dm ${window.__dmDark?'dark':''}`} style={{position: 'relative'}}>
      <Sidebar active="hosts" role="admin"/>
      <div className="dm-main">
        <Topbar crumbs={['Hôtes', host.name]} right={<DeployButtons host={host} size=""/>}/>
        <div className="dm-content dm-scroll" style={{opacity: .55}}>
          <HostInfo host={host}/>
        </div>
      </div>
      <div className="dm-overlay">
        <div className="dm-modal" style={{width: 460}}>
          <div className="dm-modal-head">
            <div style={{display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4}}>
              <span style={{width: 18, height: 18, color: 'var(--accent)', display: 'inline-flex'}}>{Icon.rocket}</span>
              <h3 style={{fontSize: 15}}>Déployer {host.name}</h3>
            </div>
            <p className="dm-small dm-muted">Confirmez les paramètres avant de lancer l'exécution.</p>
          </div>
          <div className="dm-modal-body">
            <div style={{background: 'var(--bg-subtle)', border: '1px solid var(--border)', borderRadius: 6, padding: 10, marginBottom: 14}}>
              <div className="dm-tiny dm-muted" style={{marginBottom: 2, textTransform: 'uppercase', letterSpacing: '.05em'}}>Commande exécutée</div>
              <code className="dm-mono dm-small">sh /root/{host.name}/liv.sh</code>
            </div>
            <label className="dm-label">Timeout (minutes)</label>
            <div style={{display: 'flex', gap: 8, alignItems: 'center'}}>
              <input className="dm-input" type="number" defaultValue="15" style={{width: 100}}/>
              <span className="dm-small dm-muted">Pré-rempli depuis la valeur de l'hôte (15 min)</span>
            </div>
            <div className="dm-hint" style={{display: 'flex', alignItems: 'center', gap: 6}}>
              <span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.info}</span>
              Saisissez <b>0</b> pour désactiver le timeout.
            </div>
          </div>
          <div className="dm-modal-foot">
            <button className="dm-btn">Annuler</button>
            <button className="dm-btn primary">
              <span style={{width: 12, height: 12, display: 'inline-flex'}}>{Icon.rocket}</span>
              Lancer le déploiement
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { HostDetailInline, HostDetailDrawer, HostDetailModal, DeployLaunchModal, LogsTerminal, HistoryTable, SAMPLE_DEPLOYMENTS });
