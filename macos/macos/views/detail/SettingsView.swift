//
//  SettingsView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//


import SwiftUI

struct SettingsView: View {
    
    @State private var url: String = ""
    @State private var token: String = ""
    
    @State private var launchAtStartup : Bool = false
    
    var body: some View {
        LayoutView(title: "Réglages") {
            Button("Enregistrer") {
                
            }
        } content: {
            VStack {
                CardView(title: "Connexion au serveur", isHoverable: false) {
                    Text("L'URL et le Personal Access Token sont stockés chiffrés dans le Trousseau d'accès macOS.").foregroundStyle(.secondary)
                    
                    HStack {
                        Text("URL du serveur")
                            .frame(width: 150, alignment: .leading).foregroundStyle(.secondary)
                        TextField("https://deploy.arthurbratigny.fr", text: $url)
                    }
                    
                    HStack {
                        Text("Personal Access Token")
                            .frame(width: 150, alignment: .leading).foregroundStyle(.secondary)
                        SecureField("token d'accès personnel", text: $token)
                        Button("Tester") {
                            
                        }
                    }
                    
                    HStack {
                        Text("État")
                            .frame(width: 150, alignment: .init(horizontal: .leading, vertical: .center)).foregroundStyle(.secondary)
                        CapsuleView(text: "En ligne", color: UtilColors.successColor)
                        Text("vérifié il y a 12 sc · latence 38 ms")
                            .font(Font.system(.caption, design: .monospaced))
                            .foregroundStyle(.secondary)
                    }
                }
                
                CardView(title: "Comportement", isHoverable: false) {
                    HStack {
                        Text("Lancer au démarrage")
                            
                        Spacer()
                        
                        Toggle("", isOn: $launchAtStartup)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Couper les flux SSE en veille")
                            
                        Spacer()
                        
                        Toggle("", isOn: $launchAtStartup)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Confirmer les actions via Touch ID")
                            
                        Spacer()
                        
                        Toggle("", isOn: $launchAtStartup)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Reconnecter automatiquement")
                            
                        Spacer()
                        
                        Toggle("", isOn: $launchAtStartup)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                }
                
                CardView(title: "Notifications", isHoverable: false) {
                    HStack {
                        Text("Notifier au succès d'un déploiement")
                        
                        Spacer()
                        
                        Toggle("", isOn: $launchAtStartup)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Notification en cas d'échec")
                        
                        Spacer()
                        
                        Toggle("", isOn: $launchAtStartup)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Passer l'icône en rouge si échec")
                        
                        Spacer()
                        
                        Toggle("", isOn: $launchAtStartup)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Notification via Notch")
                        
                        Spacer()
                        
                        Toggle("", isOn: $launchAtStartup)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                }
            }
        }
    }
}

#Preview {
    MainView()
}
