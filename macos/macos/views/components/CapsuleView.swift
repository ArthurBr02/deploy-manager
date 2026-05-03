//
//  CapsuleView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 03/05/2026.
//

import SwiftUI

struct CapsuleView: View {
    let text: String
    let color: Color
    
    var body: some View {
        Text(text)
            .font(.system(size: 11, weight: .bold))
            .padding(.horizontal, 10)
            .padding(.vertical, 3)
            .foregroundColor(color)
            .background(
                Capsule()
                    .stroke(color.opacity(0.1), lineWidth: 1) // Contour de la pilule
                    .background(Capsule().fill(color.opacity(0.1))) // Fond
            )
    }
}

#Preview {
    CapsuleView(text: "Test", color: .red)
}
