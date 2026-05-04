//
//  IconView.swift
//  macos
//
//  Created by Arthur BRATIGNY on 04/05/2026.
//

import SwiftUI

struct IconView: View {
    let systemName: String
    let size: CGFloat
    
    let color = UtilColors.defaultColor
    
    var body: some View {
        ZStack {
            Image(systemName: systemName)
                .font(.system(size: size/2.2))
                .foregroundColor(color)
            
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(Color.blue.opacity(0.2))
                        .frame(width: size, height: size)
        }.frame(width: size, height: size)
    }
}

#Preview {
    IconView(systemName: "server.rack", size: 50)
}
