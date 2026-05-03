//
//  AvatarView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct AvatarView: View {
    let base64String: String?
    let size: CGFloat
    
    var body: some View {
        Group {
            if let base64String,
               let uiImage = imageFromBase64(base64String) {
                Image(nsImage: uiImage) // Utilise Image(uiImage:) sur iOS
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            } else {
                // Image par défaut si le base64 est invalide ou absent
                Image(systemName: "person.circle.fill")
                    .resizable()
                    .foregroundStyle(.gray)
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle()) // Rend l'image ronde
        .overlay(Circle().stroke(Color.gray.opacity(0.2), lineWidth: 1)) // Optionnel : un petit bord
    }
    
    // Fonction utilitaire de conversion
    private func imageFromBase64(_ str: String) -> NSImage? {
        // 1. On sépare l'en-tête du contenu s'il existe
        // On cherche la virgule qui sépare "data:image/jpeg;base64," du contenu
        let components = str.components(separatedBy: ",")
        let base64Body = components.count > 1 ? components[1] : components[0]
        
        // 2. On nettoie les éventuels espaces ou retours à la ligne
        let cleanedString = base64Body.trimmingCharacters(in: .whitespacesAndNewlines)
        
        // 3. Conversion en Data
        guard let data = Data(base64Encoded: cleanedString) else {
            print("❌ Erreur: Impossible de convertir la chaîne en Data (Base64 invalide)")
            return nil
        }
        
        // 4. Création de l'image (macOS)
        return NSImage(data: data)
    }
}
