// The Swift Programming Language
// https://docs.swift.org/swift-book

import Cocoa
import SwiftRs
import UniformTypeIdentifiers

enum FileMime {
    case image
    case file
}

func text() {
    print("xxxx")
}

@_cdecl("file_chooser")
func fileChooser(type: String) {
    print("xxxx111")
    let openPanel = NSOpenPanel()
    openPanel.title = "Choose an \(type)"
    openPanel.showsResizeIndicator = true
    openPanel.showsHiddenFiles = false
    openPanel.canChooseFiles = true
    openPanel.canChooseDirectories = false
    openPanel.allowsMultipleSelection = false
    openPanel.allowedContentTypes = [UTType.jpeg, UTType.png, UTType.gif, UTType.bmp, UTType.tiff]
    print("xxxx222")
    if openPanel.runModal() == .OK {
        if let url = openPanel.url {
            print("Selected file: \(url.path)")
            // Do something with the selected image file
        }
    } else {
        print("User canceled the selection.")
    }
}


