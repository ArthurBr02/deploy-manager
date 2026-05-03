//
//  SettingsView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//


import SwiftUI
import NotchKit

struct SettingsView: View {
    @Environment(AppConfig.self) private var settings
    
    @State private var url: String = ""
    @State private var token: String = ""
    
    private let urlAccount = "server_url" //Pour Keychain
    private let tokenAccount = "api_token" //Pour Keychain
    
    @AppStorage("userProfile") var profileData: StorageWrapper<UserProfile> = StorageWrapper(nil)
    
    var user: UserProfile? {
        profileData.value
    }
    
    
    
    var body: some View {
        LayoutView(title: "Réglages") {
            
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
                        Button(action: {
                            resetUserProfileAppStorage()
                        }) {
                            Text("Réinitialiser le profil")
                        }
                        Button(action: {
                            saveToKeychain()
                            
                            Task {
                                do {
                                    NotchKit.shared.setGlow(.activity)
                                    let profile = try await ProfileService.shared.getProfile()
                                    updateProfile(newProfile: profile)
                                    print("Connecté en tant que : \(profile.firstName)")
                                    // Ici tu peux mettre à jour ton état "En ligne"
                                    NotchKit.shared.clearGlow()
                                    await NotchKit.shared.present(content: Text("Connecté en tant que : \(profile.firstName)"),
                                                                  priority: NotchPriority.high, duration: 4)
                                } catch {
                                    print("Échec : \(error)")
                                }
                            }
                            
                        }) {
                            Text("Connexion")
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
                        
                        Toggle("", isOn: settings.$launchAtStartup)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Couper les flux SSE en veille")
                            
                        Spacer()
                        
                        Toggle("", isOn: settings.$stopSseWhileSleeping)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Confirmer les actions via Touch ID")
                            
                        Spacer()
                        
                        Toggle("", isOn: settings.$confirmWithTouchID)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Reconnecter automatiquement")
                            
                        Spacer()
                        
                        Toggle("", isOn: settings.$automaticReconnection)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                }
                
                CardView(title: "Notifications", isHoverable: false) {
                    HStack {
                        Text("Notifier au succès d'un déploiement")
                        
                        Spacer()
                        
                        Toggle("", isOn: settings.$notifyOnDeployment)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Notification en cas d'échec")
                        
                        Spacer()
                        
                        Toggle("", isOn: settings.$notifyOnError)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Passer l'icône en rouge si échec")
                        
                        Spacer()
                        
                        Toggle("", isOn: settings.$changeColorOnError)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                    
                    HStack {
                        Text("Notification via Notch")
                        
                        Spacer()
                        
                        Toggle("", isOn: settings.$notifyThroughNotch)
                            .toggleStyle(.switch)
                            .labelsHidden().foregroundStyle(.secondary)
                    }
                }
            }
        }.onAppear(perform: { loadFromKeychain() })
    }
    
    private func saveToKeychain() {
        if let urlData = url.data(using: .utf8) {
            KeychainHelper.standard.save(urlData, account: urlAccount)
        }
        if let tokenData = token.data(using: .utf8) {
            KeychainHelper.standard.save(tokenData, account: tokenAccount)
        }
        print("Données sauvegardées en toute sécurité.")
    }

    private func loadFromKeychain() {
        if let urlData = KeychainHelper.standard.read(account: urlAccount),
           let savedUrl = String(data: urlData, encoding: .utf8) {
            self.url = savedUrl
        }
        
        if let tokenData = KeychainHelper.standard.read(account: tokenAccount),
           let savedToken = String(data: tokenData, encoding: .utf8) {
            self.token = savedToken
        }
    }
    
    func updateProfile(newProfile: UserProfile) {
        self.profileData = StorageWrapper(newProfile)
    }
    
    private func resetUserProfileAppStorage() {
        UserDefaults.standard.removeObject(forKey: "userProfile")
    }
}

#Preview {
    MainView()
}
