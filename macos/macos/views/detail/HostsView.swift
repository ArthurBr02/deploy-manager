//
//  HostsView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 04/05/2026.
//

import SwiftUI

struct HostsView: View {
    
    let tmp = HostModel(
        id: UUID(uuidString: "653b8537-0dd7-45c8-bbc3-7c1506cc0462")!,
        name: "fdsfrf",
        ip: "1.1.1.1",
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
        lastDeploymentStatus: "FAILURE",
        lastDeploymentAt: ISO8601DateFormatter().date(from: "2026-04-30T14:38:49.134246Z"),
        dumpEnabled: true,
        dumpFilename: "",
        isDumpAvailable: false,
        canDeploy: true,
        canEdit: true,
        canExecute: true,
        canDump: true,
        canSsh: true
    )
    
    var body: some View {
        LayoutView(title: "Hôtes") {
            HostView(host: tmp)
        }
    }
}

#Preview {
    HostsView()
}
