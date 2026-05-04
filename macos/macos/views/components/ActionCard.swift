//
//  ActionCard.swift
//  macos
//
//  Created by Arthur BRATIGNY on 04/05/2026.
//


import SwiftUI

struct ActionCard: View {
    let icon: String
    let name: String
    let description: String
    @Binding var actionSelected: String?
    
    var isSelected: Bool {
            actionSelected == name
        }
    
    var backgroundColor: Color {
        if self.isSelected {
            return .blue
        }
        return .secondary
    }
    
    var borderColor: Color {
        if self.isSelected {
            return self.backgroundColor.opacity(0.3)
        }
        return self.backgroundColor.opacity(0)
    }
    
    var body: some View {
        HStack {
            IconView(systemName: icon, size: 30)
            
            VStack(alignment: .leading) {
                Text(name).font(.headline).fontDesign(.monospaced)
                Text(description).font(.caption).fontDesign(.monospaced)
            }
        }
        .frame(maxWidth: 300, alignment: .leading)
        .padding()
        .background(backgroundColor.opacity(0.1))
        .cornerRadius(12)
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(borderColor, lineWidth: 1)
        )
        .contentShape(Rectangle())
        .onTapGesture {
            print("Élément cliqué !")
            actionSelected = self.name
        }
    }
}

#Preview {
    ActionCard(
        icon: "airplane.up.right",
        name: "Déployer",
        description: "Déploiement complet",
        actionSelected: .constant("Déployer") // Utilisation de .constant
    )
    .padding()
}
