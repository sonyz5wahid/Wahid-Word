# Wahid Word — Development Roadmap

## Phase 0 — Feasibility and foundation
- Confirm API 17 build/install/run path.
- Choose the Java/build-toolchain baseline.
- Prototype the document surface on Android 4.2.
- Evaluate DOCX parsing/writing and PDF generation libraries for API 17, size, licensing, and fidelity.
- Establish sample documents and compatibility tests.

## Phase 1 — App shell and visual system
- App icon and Wahid Word identity.
- Home/recent documents screen.
- Editor screen, title bar, Ribbon tabs, and page canvas.
- File open/save flow and basic document lifecycle.

## Phase 2 — Core editing
- Text entry and selection.
- Font and paragraph formatting.
- Undo/redo, clipboard, find/replace.
- Autosave and recovery.

## Phase 3 — DOCX and page layout
- DOCX open/save round trip.
- Page sizes, margins, orientation, headers/footers, page numbers.
- Images, tables, and hyperlinks.
- Explicit handling of unsupported DOCX features.

## Phase 4 — PDF and advanced tools
- DOCX-to-PDF export with pagination.
- Print/share workflows.
- Review tools and additional Ribbon tabs.
- Font management and document metadata.

## Phase 5 — Verification and release
- API 17 device/emulator testing.
- Tests on newer Android versions.
- DOCX compatibility and regression corpus.
- APK size and performance optimization.
- Signed release APK and installation instructions.

## Release rule

Do not label the app feature-complete until the promised workflows are implemented and tested on the minimum Android version.
