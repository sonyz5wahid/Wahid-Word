# Wahid Word — Product Specification

## 1. Product identity

- Name: Wahid Word
- Package/application identity: to be finalized during implementation.
- Visual direction: modern blue Word-inspired icon and desktop-style Office 2013 Ribbon workflow, using original Wahid Word branding and assets.
- Primary use: create, edit, format, save, and share office documents on Android phones and tablets.

## 2. Platform requirements

- Minimum SDK: API 17 (Android 4.2).
- Must not depend on Android APIs introduced after API 17 for core functionality.
- Optional newer-platform enhancements must be guarded by runtime API checks and have an API-17 fallback.
- Legacy 32-bit ARM devices are important test targets; ABI support will be confirmed against the available build toolchain and test devices.
- Size goal: approximately 50 MB, treated as an optimization target rather than a promise.

## 3. Interface

The editor should resemble a compact desktop word processor:
- Title/document bar and quick-access actions.
- Ribbon tabs and contextual tool groups.
- Home, Insert, Page Layout, References, Review, and View workflows, added in stages.
- A page canvas with visible paper boundaries and margins.
- Status bar with page count, word count, and zoom controls where practical.
- Touch-friendly controls without abandoning the desktop Ribbon visual language.

## 4. Document editing

Target capabilities:
- Text selection and insertion.
- Font family, size, bold, italic, underline, strike-through, font color, and text highlight.
- Paragraph alignment, line spacing, indentation, bullets, and numbering.
- Page size, orientation, and margins.
- Headers, footers, and page numbers.
- Tables, hyperlinks, and images.
- Find/replace, undo/redo, clipboard operations, and spell-check integration where available.

## 5. File compatibility

- Open existing DOCX files.
- Save edited documents as DOCX.
- Preserve common formatting and embedded images as faithfully as possible.
- Clearly report unsupported or lossy content rather than silently discarding it.
- Handle file selection and saving on API 17 using compatible storage/document-picker flows.
- PDF export must produce a readable, paginated PDF from the document model.

## 6. Fonts and images

- Enumerate usable system fonts where Android permits.
- Allow users to select compatible font files from storage.
- Respect font embedding/licensing limitations; do not bundle commercial fonts without permission.
- Insert, resize, align, and remove images.

## 7. Reliability

- Autosave/recovery should protect work from accidental exits.
- Large documents and images must not freeze the UI.
- Validate DOCX files before saving and test round-trip open/edit/save behavior.
- Keep a test corpus of simple and complex DOCX files.

## 8. Quality gates

A feature is complete only when:
1. It works through the visible UI.
2. It survives save, close, and reopen.
3. It behaves on Android 4.2/API 17 or is explicitly marked as newer-platform-only.
4. It has a repeatable test or documented manual verification.
