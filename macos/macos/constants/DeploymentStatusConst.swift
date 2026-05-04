//
//  DeploymentStatusConst.swift
//  macos
//
//  Created by Arthur BRATIGNY on 04/05/2026.
//

import SwiftUI

enum DeploymentStatus: String, Codable {
    // Les cases doivent matcher exactement les strings du JSON (ex: "SUCCESS")
    case pending = "PENDING"
    case inProgress = "IN_PROGRESS"
    case success = "SUCCESS"
    case failure = "FAILURE"
    case cancelled = "CANCELLED"

    // Propriété pour la couleur
    var color: Color {
        switch self {
        case .inProgress: return UtilColors.pendingColor
        case .success:    return UtilColors.successColor
        case .failure, .cancelled: return UtilColors.errorColor
        default:          return UtilColors.noneColor
        }
    }

    // Propriété pour le libellé
    var label: String {
        switch self {
        case .inProgress: return "En cours"
        case .success:    return "Succès"
        case .failure:    return "Échec"
        case .pending:    return "Jamais"
        case .cancelled:  return "Annulé"
        }
    }
}
