//
//  StorageWrapper.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import Foundation

struct StorageWrapper<T: Codable>: RawRepresentable {
    let value: T?

    var rawValue: String {
        guard let data = try? JSONEncoder().encode(value),
              let result = String(data: data, encoding: .utf8) else {
            return ""
        }
        return result
    }

    init?(rawValue: String) {
        guard let data = rawValue.data(using: .utf8),
              let result = try? JSONDecoder().decode(T.self, from: data) else {
            return nil
        }
        self.value = result
    }
    
    init(_ value: T?) {
        self.value = value
    }
}
