//
//  DeployModal.swift
//  macos
//
//  Created by Arthur BRATIGNY on 04/05/2026.
//

import SwiftUI
import NotchKit

struct DeployModal: View {
    @State var host: HostModel
    @State var actionSelected: String?
    @State var timeout: Int = 10
    
    @Environment(\.dismiss) var dismiss // Permet de fermer la modale

    var body: some View {
        let columns = [GridItem(.flexible()), GridItem(.flexible())]
        
        VStack(alignment: .leading) {
            HStack(alignment: .top) {
                IconView(systemName: "airplane.up.right", size: 35)
                VStack(alignment: .leading) {
                    Text("Lancer un déploiement").font(.headline)
                    Text("\(host.name) · \(host.ip) · \(host.domain)").fontDesign(.monospaced).font(.subheadline).foregroundStyle(.secondary)
                }
            }
            Divider()
            
            Text("Type d'opération").foregroundStyle(.secondary).font(.title3).textCase(.uppercase)
            
            VStack(alignment: .leading) {
                LazyVGrid(columns: columns, spacing: 10, content: {
                    ActionCard(icon: "airplane.up.right", name: "Déployer", description: "Déploiement complet", actionSelected: $actionSelected)
                    
                    ActionCard(icon: "hammer.fill", name: "Générer", description: "Re-générer les artefacts", actionSelected: $actionSelected)
                    
                    ActionCard(icon: "shippingbox.fill", name: "Livrer", description: "Livrer sans rebuild", actionSelected: $actionSelected)
                    
                    ActionCard(icon: "arrow.trianglehead.2.clockwise.rotate.90", name: "Rollback", description: "Revenir à la version précédente", actionSelected: $actionSelected)
                })
                
                VStack(alignment: .leading) {
                    Text("Timeout").foregroundStyle(.secondary).font(.title3).textCase(.uppercase)
                    
                    HStack {
                        TextField("Timeout", value: $timeout, format: .number)
                            .frame(width: 80)
                        
                        Text("par défaut · 10 minutes").fontDesign(.monospaced).font(.subheadline).foregroundStyle(.secondary)
                    }
                }.padding(EdgeInsets.init(top: 10, leading: 0, bottom: 10, trailing: 0))
                
                VStack(alignment: .trailing) {
                    HStack {
                        Button("Annuler") {
                            dismiss()
                        }
                        
                        Button("Confirmer & déployer", systemImage: "touchid") {
                            authenticateUser(completion: { success in
                                Task {
                                    if success {
                                        dismiss()
                                        NotchKit.shared.setGlow(.activity)
                                        Task {
                                            await NotchKit.shared.present(content: Text("Authentification réalisée avec succès. Déploiement en cours..."), priority: NotchPriority.high, duration: 4)
                                        }
                                    } else {
                                        await NotchKit.shared.present(content: Text("L'authentification a échoue. Annulation du déploiement."), priority: NotchPriority.high, duration: 4)
                                    }
                                }
                            })
                        }
                        .buttonStyle(.borderedProminent)
                        .foregroundStyle(.blue)
                        .disabled(actionSelected == nil || actionSelected!.isEmpty)
                    }
                }
            }
        }.frame(maxWidth: 1000, maxHeight: 500) // Utile sur macOS
            .padding()
    }
    
    private func action(action: String) {
        
    }
}

#Preview {
    DeployModal(host: HostModel.mock)
}
