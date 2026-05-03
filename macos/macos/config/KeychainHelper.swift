//
//  KeychainHelper.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import Foundation
import Security

class KeychainHelper {
    static let standard = KeychainHelper()
    static let service = "fr.arthurbr02.deploy-manager"
    private init() {}

    // Sauvegarder ou mettre à jour une donnée
    func save(_ data: Data, account: String) {
        let query = [
            kSecValueData: data,
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: KeychainHelper.service,
            kSecAttrAccount: account
        ] as CFDictionary

        // On tente d'ajouter l'élément
        let status = SecItemAdd(query, nil)

        if status == errSecDuplicateItem {
            // Si l'item existe déjà, on le met à jour
            let queryToUpdate = [
                kSecClass: kSecClassGenericPassword,
                kSecAttrService: KeychainHelper.service,
                kSecAttrAccount: account
            ] as CFDictionary

            let attributesToUpdate = [kSecValueData: data] as CFDictionary
            SecItemUpdate(queryToUpdate, attributesToUpdate)
        }
    }

    // Lire une donnée
    func read(account: String) -> Data? {
        let query = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: KeychainHelper.service,
            kSecAttrAccount: account,
            kSecReturnData: true
        ] as CFDictionary

        var result: AnyObject?
        SecItemCopyMatching(query, &result)
        return result as? Data
    }

    // Supprimer une donnée
    func delete(account: String) {
        let query = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: KeychainHelper.service,
            kSecAttrAccount: account
        ] as CFDictionary

        SecItemDelete(query)
    }
}
