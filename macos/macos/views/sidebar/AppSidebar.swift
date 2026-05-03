//
//  AppSidebar.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct AppSidebar: View {
    @Bindable var navManager: NavigationManager

    var body: some View {
        List(selection: $navManager.selection) {
            Section("Général") {
                Label("Accueil", systemImage: "house").tag(NavigationItem.home)
                Label("Réglages", systemImage: "gear").tag(NavigationItem.settings)
            }
            
            Section("Mes Dossiers") {
                Label("Projets", systemImage: "folder").tag(NavigationItem.folder(id: "projets"))
            }
        }
        .listStyle(.sidebar)
    }
}
