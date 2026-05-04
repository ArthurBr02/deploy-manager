//
//  HostModel.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import Foundation

struct HostModel: Identifiable, Codable {
    let id: UUID
    let name: String
    let ip: String
    let domain: String
    let sshUser: String
    let sshPort: Int
    
    // Champs optionnels (peuvent être null dans le JSON)
    let deploymentCommand: String?
    let generateCommand: String?
    let deliverCommand: String?
    let tlogCommand: String?
    let rollbackCommand: String?
    let healthcheckUrl: String?
    let dumpCommand: String?
    let dumpFolder: String?
    let defaultTimeout: Int?
    let lastDeploymentStatus: String?
    let lastDeploymentAt: Date?
    
    // Valeurs booléennes
    let dumpEnabled: Bool
    let dumpFilename: String
    let isDumpAvailable: Bool
    let canDeploy: Bool
    let canEdit: Bool
    let canExecute: Bool
    let canDump: Bool
    let canSsh: Bool
}
