//
//  AppConfig.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

import SwiftUI
import Observation

@Observable
class AppConfig {
    // On centralise toutes les clés dans une Enum pour éviter les fautes de frappe
    private enum Keys {
        static let launchAtStartup = "launchAtStartup"
        static let stopSseWhileSleeping = "stopSseWhileSleeping"
        static let confirmWithTouchID = "confirmWithTouchID"
        static let automaticReconnection = "automaticReconnection"
        static let notifyOnDeployment = "notifyOnDeployment"
        static let notifyOnError = "notifyOnError"
        static let changeColorOnError = "changeColorOnError"
        static let notifyThroughNotch = "notifyThroughNotch"
    }

    // Propriétés persistantes
    @ObservationIgnored @AppStorage(Keys.launchAtStartup) var launchAtStartup = false
    @ObservationIgnored @AppStorage(Keys.stopSseWhileSleeping) var stopSseWhileSleeping = false
    @ObservationIgnored @AppStorage(Keys.confirmWithTouchID) var confirmWithTouchID = false
    @ObservationIgnored @AppStorage(Keys.automaticReconnection) var automaticReconnection = false
    @ObservationIgnored @AppStorage(Keys.notifyOnDeployment) var notifyOnDeployment = false
    @ObservationIgnored @AppStorage(Keys.notifyOnError) var notifyOnError = false
    @ObservationIgnored @AppStorage(Keys.changeColorOnError) var changeColorOnError = false
    @ObservationIgnored @AppStorage(Keys.notifyThroughNotch) var notifyThroughNotch = false
}
