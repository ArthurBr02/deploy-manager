//
//  HostView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct HostView: View {
    let host: HostModel
    
    @State private var isShowingDeployModal = false
    
    var status: DeploymentStatus {
        DeploymentStatus(rawValue: host.lastDeploymentStatus ?? "PENDING") ?? .pending
    }
    
    var formattedDate: String? {
        host.lastDeploymentAt?.formatted(date: .numeric, time: .shortened)
    }
    
    var body: some View {
        CardView(isHoverable: false) {
            VStack(alignment: .leading) {
                HStack {
                    IconView(systemName: "server.rack", size: 40)
                    VStack(alignment: .leading) {
                        HStack {
                            Text(host.name).font(.headline)
                            CapsuleView(text: status.label, color: status.color)
                        }
                        
                        HStack {
                            if (host.ip != "") {
                                Text(host.ip).fontDesign(.monospaced).font(.subheadline).foregroundStyle(.secondary)
                            }
                            
                            if (host.ip != "" && host.domain != "") { Text("·").fontDesign(.monospaced).font(.subheadline).foregroundStyle(.secondary)
                            }
                            
                            if (host.domain != "") {
                                Text(host.domain).fontDesign(.monospaced).font(.subheadline).foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                
                Divider()
                
                if let dateText = formattedDate {
                    HStack {
                        Text("Dernier déploiement le \(dateText)")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        
                        // TODO: Ajouter le nom de la personne ayant déployé Text(host.)
                    }
                }
                
                HStack {
                    Button(action: {
                        isShowingDeployModal = true
                    }) {
                        Label("Déployer", systemImage: "airplane.up.right")
                                .frame(maxWidth: .infinity)
                    }.help("Cliquez pour déployer l'application sur l'hôte")
                        .foregroundStyle(.deploy)
                        .sheet(isPresented: $isShowingDeployModal) {
                            DeployModal(host: host)
                        }
                    
                    if host.canSsh {
                        Button(action: {}) {
                            Image(systemName: "terminal")
                        }.help("Cliquez pour vous connecter sur l'hôte")
                    }
                    
                    Button(action: {}) {
                        Image(systemName: "clock.arrow.circlepath")
                    }.help("Cliquez pour accéder à l'historique de déploiement de l'hôte")
                }
            }
        }
    }
}

extension HostModel {
    static var mock: HostModel {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        return HostModel(
            id: UUID(uuidString: "653b8537-0dd7-45c8-bbc3-7c1506cc0462")!,
            name: "ansible-prod",
            ip: "192.168.1.34",
            domain: "dfs.fr",
            sshUser: "root",
            sshPort: 22,
            deploymentCommand: nil,
            generateCommand: nil,
            deliverCommand: nil,
            tlogCommand: nil,
            rollbackCommand: nil,
            healthcheckUrl: nil,
            dumpCommand: "gfhdhgf > gfgfdgfd",
            dumpFolder: nil,
            defaultTimeout: nil,
            // Les arguments qui te manquaient :
            lastDeploymentStatus: "FAILURE",
            lastDeploymentAt: formatter.date(from: "2026-04-30T14:38:49.134246Z"),
            dumpEnabled: true,
            dumpFilename: "backup.sql",
            isDumpAvailable: false,
            canDeploy: true,
            canEdit: true,
            canExecute: true,
            canDump: true,
            canSsh: true
        )
    }
}

#Preview {
    // On crée une closure qui s'auto-exécute pour préparer les données
    let host: HostModel = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        
        return HostModel.mock
    }()

    HostView(host: host)
        .padding()
}
