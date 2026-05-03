//
//  MainView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct MainView: View {
    @State private var navManager = NavigationManager()

    var body: some View {
        NavigationSplitView {
            AppSidebar(navManager: navManager)
        } detail: {
            AppRouter(selection: navManager.selection)
        }
    }
}

#Preview {
    MainView()
}
