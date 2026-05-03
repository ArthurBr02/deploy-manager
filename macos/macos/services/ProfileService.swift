//
//  ProfileService.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import Foundation

class ProfileService {
    static let shared = ProfileService()
    private init() {}

    private let urlAccount = "server_url"
    private let tokenAccount = "api_token"

    func getProfile() async throws -> UserProfile {
        guard
            let urlData = KeychainHelper.standard.read(account: urlAccount),
            let tokenData = KeychainHelper.standard.read(account: tokenAccount),
            let savedUrlStr = String(data: urlData, encoding: .utf8),
            let token = String(data: tokenData, encoding: .utf8)
        else {
            throw ApiError.unauthorized
        }

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601

        return try await ApiService.shared.get(
            path: "/api/profile",
            baseURL: savedUrlStr,
            token: token,
            decoder: decoder,
            timeout: 10
        )
    }
}
