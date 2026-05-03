//
//  UserProfile.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import Foundation

struct UserProfile: Codable {
    let id: String
    let email: String
    let firstName: String
    let lastName: String
    let role: String
    let avatar: String? // Optionnel au cas où l'avatar est absent
    let createdAt: Date
    
    // Pour afficher le nom complet facilement
    var fullName: String {
        "\(firstName) \(lastName)"
    }
}
