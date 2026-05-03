//
//  Card.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct CardView<Content: View>: View {
    // Propriétés de configuration
    let title: String?
    let icon: String?
    let isHoverable: Bool // Nouvelle propriété
    let content: Content
    
    // État interne pour l'animation
    @State private var isHovered: Bool = false

    // Initialiseur mis à jour
    init(
        title: String? = nil,
        icon: String? = nil,
        isHoverable: Bool = true, // Activé par défaut
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.icon = icon
        self.isHoverable = isHoverable
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // En-tête (Si titre ou icône présents)
            if title != nil || icon != nil {
                HStack(spacing: 8) {
                    if let icon = icon {
                        Image(systemName: icon)
                            .foregroundColor(.accentColor)
                            .font(.system(size: 14, weight: .semibold))
                    }
                    if let title = title {
                        Text(title)
                            .font(.headline)
                    }
                }
            }

            // Corps de la carte
            content
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        
        // --- STYLE NATIF ---
        .background(Color(NSColor.controlBackgroundColor))
        .cornerRadius(12)
        
        // --- LOGIQUE D'ÉLÉVATION CONDITIONNELLE ---
        .shadow(
            color: Color.black.opacity(isHovered && isHoverable ? 0.15 : 0.08),
            radius: isHovered && isHoverable ? 15 : 8,
            x: 0,
            y: isHovered && isHoverable ? 8 : 4
        )
        // Petit zoom uniquement si isHoverable est vrai
        .scaleEffect(isHovered && isHoverable ? 1.015 : 1.0)
        
        // --- GESTION DU SURVOL ---
        .onHover { hovering in
            if isHoverable {
                withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                    isHovered = hovering
                }
            }
        }
        // Change le curseur en main pointante si la carte est "hoverable"
        .help(isHoverable ? "Cliquez pour interagir" : "") // Optionnel : Tooltip
    }
}

#Preview {
    MainView()
}
