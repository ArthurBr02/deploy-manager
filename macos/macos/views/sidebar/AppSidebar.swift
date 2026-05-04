//
//  AppSidebar.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct AppSidebar: View {
    @Bindable var navManager: NavigationManager
    
    @AppStorage("userProfile") var profileData: StorageWrapper<UserProfile> = StorageWrapper(nil)
    var user: UserProfile? {
        profileData.value
    }
    
    @AppStorage("favoris") var favoris: Bool = false

    var body: some View {
        List(selection: $navManager.selection) {
            Section("Espace de travail") {
                Label("Hôtes", systemImage: "server.rack").tag(NavigationItem.hosts)
                Label("Réglages", systemImage: "gear").tag(NavigationItem.settings)
            }
            
            if favoris {
                Section("Favoris") {
                    Label("Projets", systemImage: "folder").tag(NavigationItem.folder(id: "projets"))
                }
            }
        }
        .listStyle(.sidebar)
        
        if (user != nil) {
            Spacer()
            Divider()
            
            HStack {
                AvatarView(base64String: user!.avatar, size: 20)
                Text(user!.fullName)
            }.padding(EdgeInsets.init(top: 10, leading: 0, bottom: 10, trailing: 0))
        }
    
    }
}
