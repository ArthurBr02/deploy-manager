//
//  FolderViewMdel.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import Foundation

@Observable
class FolderViewModel {
    var folderName: String = ""
    var files: [String] = []
    
    func loadData(for id: String) {
        // Simulation d'un chargement de données (ex: CoreData ou API)
        self.folderName = "Projets \(id)"
        self.files = ["Specs.pdf", "Design.sketch"]
    }
}
