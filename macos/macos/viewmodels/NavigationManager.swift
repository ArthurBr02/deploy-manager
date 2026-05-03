//
//  NavigationManager.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

enum NavigationItem: Hashable {
    case home
    case settings
    case folder(id: String)
}

@Observable
class NavigationManager {
    var selection: NavigationItem? = .settings
}
