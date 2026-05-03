//
//  AppRouter.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct AppRouter: View {
    let selection: NavigationItem?

    var body: some View {
        Group {
            switch selection {
            case .home:
                HomeView()
            case .settings:
                SettingsView()
            case .folder(let id):
                FolderDetailView(folderId: id)
            case .none:
                Text("Sélectionnez un élément")
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}
