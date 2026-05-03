//
//  LayoutView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct LayoutView<HeaderActions: View, Content: View>: View {
    let title: String
    let headerActions: HeaderActions // Pour les boutons à droite
    let content: Content             // Pour le contenu sous le séparateur
    
    // Initialiseur flexible
    init(
        title: String,
        @ViewBuilder headerActions: () -> HeaderActions = { EmptyView() },
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.headerActions = headerActions()
        self.content = content()
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            // --- HEADER ---
            HStack(alignment: .top) {
                Text(title)
                    .font(.headline)
                    .foregroundColor(.primary)
                
                Spacer()
                
                // Zone pour les boutons dynamiques
                headerActions
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            
            // --- SÉPARATEUR ---
            Divider()
                .opacity(0.6) // Un peu plus discret pour le look macOS
            
            // --- CONTENU ---
            
                ScrollView {
            VStack(alignment: .leading) {
                    content
                }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        // --- STYLE DE LA CARTE ---
        .background(Color(NSColor.controlBackgroundColor))
        .cornerRadius(12)
        // Ajout d'une ombre légère pour l'élévation demandée précédemment
        .shadow(color: Color.black.opacity(0.08), radius: 8, x: 0, y: 4)
    }
}

// --- PREVIEW ---
#Preview {
    VStack {
        LayoutView(title: "Statistiques") {
            // Section headerActions (à droite)
            HStack(spacing: 8) {
                Button(action: {}) {
                    Image(systemName: "arrow.clockwise")
                }
                .buttonStyle(.plain) // Style discret pour le header
                
                Button("Exporter") { }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            }
        } content: {
            // Section contenu (sous le trait)
            VStack(alignment: .leading, spacing: 8) {
                Text("Données d'utilisation")
                    .font(.subheadline)
                    .bold()
                Text("85% de l'espace disque utilisé.")
                    .foregroundColor(.secondary)
            }
        }
        .frame(width: 400)
    }.padding(40)
}
