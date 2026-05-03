//
//  ApiService.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import Foundation

enum ApiError: Error {
    case invalidURL
    case requestFailed
    case decodingError
    case unauthorized
}

class ApiService {
    static let shared = ApiService()
    private init() {}

    func get<T: Decodable>(
        path: String,
        baseURL: String,
        token: String,
        decoder: JSONDecoder = JSONDecoder(),
        timeout: TimeInterval = 30
    ) async throws -> T {
        try await request(method: "GET", path: path, baseURL: baseURL, token: token, body: nil as Data?, decoder: decoder, timeout: timeout)
    }

    func post<B: Encodable, T: Decodable>(
        path: String,
        baseURL: String,
        token: String,
        body: B,
        decoder: JSONDecoder = JSONDecoder(),
        timeout: TimeInterval = 30
    ) async throws -> T {
        let bodyData = try encodeBody(body)
        return try await request(method: "POST", path: path, baseURL: baseURL, token: token, body: bodyData, decoder: decoder, timeout: timeout)
    }

    func put<B: Encodable, T: Decodable>(
        path: String,
        baseURL: String,
        token: String,
        body: B,
        decoder: JSONDecoder = JSONDecoder(),
        timeout: TimeInterval = 30
    ) async throws -> T {
        let bodyData = try encodeBody(body)
        return try await request(method: "PUT", path: path, baseURL: baseURL, token: token, body: bodyData, decoder: decoder, timeout: timeout)
    }

    func delete(
        path: String,
        baseURL: String,
        token: String,
        timeout: TimeInterval = 30
    ) async throws {
        let _: EmptyResponse = try await request(method: "DELETE", path: path, baseURL: baseURL, token: token, body: nil as Data?, decoder: JSONDecoder(), timeout: timeout)
    }

    // MARK: - Private

    private func request<T: Decodable>(
        method: String,
        path: String,
        baseURL: String,
        token: String,
        body: Data?,
        decoder: JSONDecoder,
        timeout: TimeInterval
    ) async throws -> T {
        guard let base = URL(string: baseURL),
              let url = URL(string: path, relativeTo: base) else {
            throw ApiError.invalidURL
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = method
        urlRequest.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        urlRequest.setValue("application/json", forHTTPHeaderField: "Accept")
        urlRequest.timeoutInterval = timeout

        if let body {
            urlRequest.httpBody = body
            urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }

        let (data, response) = try await URLSession.shared.data(for: urlRequest)

        guard let httpResponse = response as? HTTPURLResponse else {
            throw ApiError.requestFailed
        }

        if httpResponse.statusCode == 401 { throw ApiError.unauthorized }
        guard (200..<300).contains(httpResponse.statusCode) else { throw ApiError.requestFailed }
        print(data, T.self)
        do {
            let finalData = data.isEmpty ? "{}".data(using: .utf8)! : data
            return try decoder.decode(T.self, from: finalData)
        } catch let decodingError {
            print("❌ Erreur de décodage sur \(T.self): \(decodingError)") // <--- TRÈS IMPORTANT
            throw ApiError.decodingError
        }
    }

    private func encodeBody<B: Encodable>(_ body: B) throws -> Data {
        do {
            return try JSONEncoder().encode(body)
        } catch {
            throw ApiError.requestFailed
        }
    }
}

private struct EmptyResponse: Decodable {}
