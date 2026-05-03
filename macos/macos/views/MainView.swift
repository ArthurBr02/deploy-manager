//
//  MainView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct MainView: View {
    @State private var navManager = NavigationManager()
    @State private var settings = AppConfig()

    var body: some View {
        NavigationSplitView {
            AppSidebar(navManager: navManager)
        } detail: {
            AppRouter(selection: navManager.selection)
        }.environment(settings)
    }
}

#Preview {
    MainView()
}
