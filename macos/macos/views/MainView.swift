//
//  MainView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI
import NotchKit

struct MainView: View {
    @State private var navManager = NavigationManager()
    @State private var settings = AppConfig()

    var body: some View {
        NavigationSplitView {
            AppSidebar(navManager: navManager)
        } detail: {
            AppRouter(selection: navManager.selection)
        }.environment(settings)
            .onAppear {
                Task { @MainActor in
                    NotchKit.shared.configure { config in
                        config.defaultDismissDelay = 4.0
                        config.hoverProximity = 25.0
                    }
                    NotchKit.shared.start()
                }
            }
    }
}

#Preview {
    MainView()
}
