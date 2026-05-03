//
//  FolderDetailView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct FolderDetailView: View {
    let folderId: String
    @State private var viewModel = FolderViewModel()

    var body: some View {
        List(viewModel.files, id: \.self) { file in
            Text(file)
        }
        .navigationTitle(viewModel.folderName)
        .onAppear {
            // On charge les données quand la vue s'affiche
            viewModel.loadData(for: folderId)
        }
    }
}

#Preview {
    NavigationStack {
        FolderDetailView(folderId: "Travail")
    }
}
