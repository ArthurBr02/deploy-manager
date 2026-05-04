//
//  AuthManager.swift
//  macos
//
//  Created by Arthur BRATIGNY on 04/05/2026.
//

import LocalAuthentication

func authenticateUser(reason: String = "vous authentifier pour réaliser l'action voulue.", completion: @escaping (Bool) -> Void) {
    let context = LAContext()
    var error: NSError?

    if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {

        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, authenticationError in
            DispatchQueue.main.async {
                if success {
                    completion(true)
                } else {
                    completion(false)
                }
            }
        }
    } else {
        print("Biométrie non disponible : \(error?.localizedDescription ?? "Erreur inconnue")")
        completion(false)
    }
}
