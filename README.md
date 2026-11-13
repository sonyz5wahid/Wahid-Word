# Wahid Word

Wahid Word is an independent Android word processor inspired by the desktop workflow of Microsoft Word 2013. It is not a copy of Microsoft Word or WordCraft.

## Product goal

Build a practical, feature-rich document editor with a familiar desktop-style Ribbon interface, while keeping Android 4.2 (API 17) as the minimum supported platform.

## Compatibility targets

- Minimum Android version: Android 4.2 / API 17.
- APK size: aim for approximately 50 MB where technically practical; actual size depends on the document engine, fonts, native libraries, and ABI packaging.
- Legacy-device support is a first-class requirement. Modern Android-only APIs and libraries must not be required by the core editor.
- Test on API 17 and newer Android versions before calling a feature supported.

## Intended feature set

- Desktop-inspired Ribbon tabs: File, Home, Insert, Layout, Review, and View.
- Rich text formatting: font family and size, bold, italic, underline, color, highlighting, alignment, lists, indentation, and paragraph spacing.
- Page setup: A4 and other paper sizes, margins, orientation, page numbering, headers, and footers.
- DOCX open, edit, and save.
- Insert and resize images.
- Access compatible system and user-installed fonts.
- Find/replace, selection, clipboard, undo, and redo.
- Export DOCX documents to PDF and print where the platform supports it.
- Document recovery and autosave.

The feature list is a product target. Each feature must be implemented and tested; a menu item alone does not count as a working feature.

## Development approach

The ArtCraft/WordCraft Android project is a reference for desktop-style interaction and document-app workflows, not a drop-in implementation for API 17. Its current Android builder uses a modern native stack and 64-bit ARM packaging, so Wahid Word will be designed as a separate legacy-compatible application.

The initial technical direction is a lightweight Java Android shell with a document-editing surface and a separately tested DOCX/PDF pipeline. The exact editing and conversion libraries will be selected only after an API-17 compatibility and document-fidelity prototype.

## Project status

Planning and compatibility investigation. No installable APK is available yet.

See [Product Specification](docs/PRODUCT_SPEC.md) and [Roadmap](docs/ROADMAP.md).
